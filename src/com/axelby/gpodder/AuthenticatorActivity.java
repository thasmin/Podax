package com.axelby.gpodder;

import com.axelby.podax.Constants;
import com.axelby.podax.GPodderProvider;
import com.axelby.podax.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {
	public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	String _username;
	String _password;
	String _authtokenType;
	boolean _requestNewAccount;
	boolean _confirmCredentials;

	private final Handler _handler = new Handler();
	private Thread _authThread;

	private TextView _messageText;
	private EditText _usernameEdit;
	private EditText _passwordEdit;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		final Intent intent = getIntent();
        _username = intent.getStringExtra(PARAM_USERNAME);
        _authtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
        _requestNewAccount = _username == null;
        _confirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);

        setContentView(R.layout.gpodder_login);
        _messageText = (TextView)findViewById(R.id.message);
        _usernameEdit = (EditText)findViewById(R.id.username_edit);
        _passwordEdit = (EditText)findViewById(R.id.password_edit);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Logging into GPodder...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (_authThread != null) {
                    _authThread.interrupt();
                    finish();
                }
            }
        });
        return dialog;
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
        showDialog(0);
    }

    protected void hideProgress() {
        dismissDialog(0);
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
					try {
						new Runnable() {
							public void run() {
								final boolean isValid = client.authenticate();
								_handler.post(new Runnable() {
									public void run() {
										onAuthenticationResult(isValid);
									}
								});
							}
						}.run();
					} finally {
					}
				}
			};
			_authThread.start();
		}
	}

	public void onAuthenticationResult(boolean isValid) {
		hideProgress();
		if (!isValid) {
			if (_requestNewAccount) {
				_messageText.setText("That username and password did not work on GPodder.");
			} else {
				_messageText.setText("That password did not work on GPodder.");
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
			// if this is our first account, ask for everyone to tell us about their subscriptions
			if (accountManager.getAccountsByType(Constants.GPODDER_ACCOUNT_TYPE).length == 0) {
				Intent intent = new Intent("com.axelby.gpodder.INITIALIZE");
				sendBroadcast(intent);
			}
			accountManager.addAccountExplicitly(account, _password, null);
			// Set contacts sync for this account.
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
}
