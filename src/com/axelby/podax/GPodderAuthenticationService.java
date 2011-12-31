package com.axelby.podax;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class GPodderAuthenticationService extends Service {

	private GPodderAuthenticator _authenticator;

	@Override
	public void onCreate() {
		super.onCreate();
		_authenticator = new GPodderAuthenticator(this);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return _authenticator.getIBinder();
	}

	private static class GPodderAuthenticator extends AbstractAccountAuthenticator {
		private final Context _context;

		public GPodderAuthenticator(Context context) {
			super(context);
			_context = context;
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,
				String accountType, String authTokenType, String[] requredFeatures,
				Bundle options) throws NetworkErrorException {
			final Intent intent = new Intent(_context, GPodderAuthenticatorActivity.class);
			intent.putExtra(GPodderAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			final Bundle bundle = new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			return bundle;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,
				Account account, Bundle options) throws NetworkErrorException {
			if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
				final String password = options.getString(AccountManager.KEY_PASSWORD);
				GPodderClient client = new GPodderClient(account.name, password);
				final boolean verified = client.authenticate();
				final Bundle result = new Bundle();
				result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
				return result;
			}
			// Launch AuthenticatorActivity to confirm credentials
			final Intent intent = new Intent(_context, GPodderAuthenticatorActivity.class);
			intent.putExtra(GPodderAuthenticatorActivity.PARAM_USERNAME, account.name);
			intent.putExtra(GPodderAuthenticatorActivity.PARAM_CONFIRMCREDENTIALS, true);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			final Bundle bundle = new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			return bundle;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,
				String accountType) {
			throw new UnsupportedOperationException();
		}

		/*
		 * GPodder uses HTTP authentication which does not involve a token
		 * (non-Javadoc)
		 * @see android.accounts.AbstractAccountAuthenticator#getAuthToken(android.accounts.AccountAuthenticatorResponse, android.accounts.Account, java.lang.String, android.os.Bundle)
		 */
		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,
				Account account, String[] features) throws NetworkErrorException {
			final Bundle result = new Bundle();
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
			return result;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			final Intent intent = new Intent(_context, GPodderAuthenticatorActivity.class);
			intent.putExtra(GPodderAuthenticatorActivity.PARAM_USERNAME, account.name);
			intent.putExtra(GPodderAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
			intent.putExtra(GPodderAuthenticatorActivity.PARAM_CONFIRMCREDENTIALS, false);
			final Bundle bundle = new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			return bundle;
		}
	}
}
