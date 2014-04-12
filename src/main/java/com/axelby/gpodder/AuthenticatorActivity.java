package com.axelby.gpodder;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.GPodderProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.ui.ProgressDialogFragment;

public class AuthenticatorActivity extends FragmentActivity {
	public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	private final Handler _handler = new Handler();
	String _username;
	String _password;
	String _authtokenType;
	boolean _requestNewAccount;
	boolean _confirmCredentials;
	private Thread _authThread;
	private TextView _messageText;
	private EditText _usernameEdit;
	private EditText _passwordEdit;
	private EditText _deviceNameEdit;
	private RadioGroup _deviceTypeList;
	private AccountAuthenticatorResponse _accountAuthenticatorResponse;
	private Bundle _resultBundle;
	private ProgressDialogFragment _progressDialog;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		final Intent intent = getIntent();
		_username = intent.getStringExtra(PARAM_USERNAME);
		_authtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
		_requestNewAccount = _username == null;
		_confirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);

		setContentView(R.layout.gpodder_login);
		_messageText = (TextView) findViewById(R.id.message);
		_usernameEdit = (EditText) findViewById(R.id.username);
		_passwordEdit = (EditText) findViewById(R.id.password);
		_deviceNameEdit = (EditText) findViewById(R.id.devicename);
		_deviceTypeList = (RadioGroup) findViewById(R.id.devicetype);
		_deviceTypeList.check(Helper.isTablet(this) ? R.id.radioLaptop : R.id.radioMobile);

		// taken from AccountAuthenticatorActivity source code
		_accountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
		if (_accountAuthenticatorResponse != null)
			_accountAuthenticatorResponse.onRequestContinued();
	}

	private CharSequence getMessage() {
		if (TextUtils.isEmpty(_username)) {
			// If no username, then we ask the user to log in using an
			// appropriate service.
			return "New Account";
		}
		if (TextUtils.isEmpty(_password)) {
			// We have an account but no password
			return "Password missing";
		}
		return null;
	}

	protected void showProgress() {
		FragmentManager fm = getSupportFragmentManager();
		_progressDialog = ProgressDialogFragment.newInstance();
		_progressDialog.show(fm, "progress");
	}

	protected void hideProgress() {
		_progressDialog.dismiss();
	}

	public void handleLogin(View view) {
		if (_requestNewAccount) {
			_username = _usernameEdit.getText().toString();
		}
		_password = _passwordEdit.getText().toString();
		if (TextUtils.isEmpty(_username) || TextUtils.isEmpty(_password)) {
			_messageText.setText(getMessage());
		} else {
			showProgress();
			final Client client = new Client(this, _username, _password);

			_authThread = new Thread() {
				@Override
				public void run() {
					new Runnable() {
						public void run() {
							final boolean isValid = client.authenticate();
							SharedPreferences gpodderPrefs = getSharedPreferences("gpodder", MODE_PRIVATE);
							gpodderPrefs.edit()
									.putString("caption", _deviceNameEdit.getText().toString())
									.putString("type", getCheckedDeviceType())
									.commit();

							_handler.post(new Runnable() {
								public void run() {
									onAuthenticationResult(isValid);
								}
							});
						}
					}.run();
				}
			};
			_authThread.start();
		}
	}

	private String getCheckedDeviceType() {
		switch (_deviceTypeList.getCheckedRadioButtonId()) {
			case R.id.radioDesktop: return "desktop";
			case R.id.radioLaptop: return "laptop";
			case R.id.radioMobile: return "mobile";
			case R.id.radioServer: return "server";
			case R.id.radioOther: return "other";
			default: return "mobile";
		}
	}

	public void onAuthenticationResult(boolean isValid) {
		hideProgress();
		if (!isValid) {
			if (_requestNewAccount) {
				_messageText.setText("That username and password did not work on gpodder.net.");
			} else {
				_messageText.setText("That password did not work on gpodder.net.");
			}
		} else {
			if (_confirmCredentials) {
				finishConfirmCredentials(isValid);
			} else {
				finishLogin();
			}
		}
	}

	private void finishLogin() {
		final Account account = new Account(_username, Constants.GPODDER_ACCOUNT_TYPE);

		AccountManager accountManager = AccountManager.get(this);
		if (_requestNewAccount) {
			String rdm = Long.toHexString(Double.doubleToLongBits(Math.random()));
			SharedPreferences gpodderPrefs = getSharedPreferences("gpodder", MODE_PRIVATE);
			gpodderPrefs.edit()
					.putString("deviceId", "podax_" + rdm)
					.putBoolean("configurationNeedsUpdate", true)
					.commit();

			accountManager.addAccountExplicitly(account, _password, null);
			ContentResolver.requestSync(account, GPodderProvider.AUTHORITY, new Bundle());
			ContentResolver.setSyncAutomatically(account, GPodderProvider.AUTHORITY, true);
		} else {
			accountManager.setPassword(account, _password);
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, _username);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.GPODDER_ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	protected void finishConfirmCredentials(boolean result) {
		final Account account = new Account(_username, Constants.GPODDER_ACCOUNT_TYPE);
		AccountManager accountManager = AccountManager.get(this);
		accountManager.setPassword(account, _password);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	public void setAccountAuthenticatorResult(Bundle accountAuthenticatorResult) {
		this._resultBundle = accountAuthenticatorResult;
	}

	@Override
	public void finish() {
		// copied from AccountAuthenticatorActivity source code
		if (_accountAuthenticatorResponse != null) {
			// send the result bundle back if set, otherwise send an error.
			if (_resultBundle != null)
				_accountAuthenticatorResponse.onResult(_resultBundle);
			else
				_accountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
			_accountAuthenticatorResponse = null;
		}
		super.finish();
	}

}
