package com.axelby.podax;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;

public class GoogleAccountChooserActivity extends ListActivity {
	protected AccountManager _accountManager;
	protected Account[] _accounts;
	protected Account _chosenAccount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_accountManager = AccountManager.get(getApplicationContext());
		_chosenAccount = null;
		_accountManager.getAccountsByTypeAndFeatures("com.google", new String[] { "service_reader" },
				new AccountManagerCallback<Account[]>() {
					public void run(AccountManagerFuture<Account[]> future) {
						try {
							_accounts = future.getResult();

							String[] names = new String[_accounts.length];
							for (int i = 0; i < _accounts.length; ++i)
								names[i] = _accounts[i].name;
							GoogleAccountChooserActivity.this.setListAdapter(
									new ArrayAdapter<String>(GoogleAccountChooserActivity.this,
											android.R.layout.simple_list_item_1, names));
						} catch (OperationCanceledException e) {
							Log.e("Podax", "Operation Canceled", e);
						} catch (IOException e) {
							Log.e("Podax", "IOException", e);
						} catch (AuthenticatorException e) {
							Log.e("Podax", "Authentication Failed", e);
						}
					}
			}, null);
	}

	@Override
	protected void onListItemClick(ListView l, View v, final int position, long itemId) {
		_chosenAccount = _accounts[position];
		getAuthToken();
	}

	private void getAuthToken() {
		if (_chosenAccount == null)
			return;

		_accountManager.getAuthToken(_chosenAccount, "reader", true, new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				Bundle authTokenBundle = null;
				try {
					authTokenBundle = future.getResult();

					if (authTokenBundle == null)
						return;

					if(authTokenBundle.containsKey(AccountManager.KEY_INTENT)) {
						// User input required
						Intent intent = (Intent)authTokenBundle.get(AccountManager.KEY_INTENT);

						// clear the new task flag
						int flags = intent.getFlags();
					    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
					    intent.setFlags(flags);

						startActivityForResult(intent, 1);
						return;
					}

					doImport(authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN));
				} catch (OperationCanceledException e) {
					Log.e("Podax", "Operation Canceled", e);
				} catch (IOException e) {
					Log.e("Podax", "IOException", e);
				} catch (AuthenticatorException e) {
					Log.e("Podax", "Authentication Failed", e);
				}
			}
		}, 
		null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1)
		{
			if (resultCode == RESULT_OK && data == null)
				getAuthToken();
		}
	}

	private void doImport(String authToken) {
		HttpTransport transport = GoogleTransport.create();

		GoogleHeaders headers = (GoogleHeaders) transport.defaultHeaders;
		headers.setApplicationName("Podax");
		headers.gdataVersion = "2";
		headers.setGoogleLogin(authToken);

		HttpRequest request = transport.buildGetRequest();
		request.url = new GenericUrl("http://www.google.com/reader/api/0/subscription/list");
		try {
			InputStream response = request.execute().getContent();

			// set up the sax parser
			RootElement root = new RootElement("object");
			Element object = root.getChild("list").getChild("object");
			// put these in an array to we can use them in the inner class
			final String[] id = { "" };
			final String[] title = { "" };
			final String[] stringType = { "" };
			final DBAdapter adapter = DBAdapter.getInstance(GoogleAccountChooserActivity.this);

			object.setElementListener(new ElementListener() {
				public void start(Attributes attrs) {
					id[0] = "";
					title[0] = "";
				}
				public void end() {
					Subscription subscription = adapter.addSubscription(id[0], title[0]);
					UpdateService.updateSubscription(GoogleAccountChooserActivity.this, subscription);
				}
			});

			object.getChild("string").setStartElementListener(new StartElementListener() {
				public void start(Attributes attributes) {
					stringType[0] = attributes.getValue("name");
				}
			});

			object.getChild("string").setEndTextElementListener(new EndTextElementListener() {
				public void end(String text) {
					if (stringType[0].equals("id")) {
						if (text != null && text.startsWith("feed/"))
							text = text.substring(5);
						id[0] = text;
					}
					else if (stringType[0].equals("title"))
						title[0] = text;
				}
			});

			Xml.parse(response, Xml.Encoding.UTF_8, root.getContentHandler());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		Toast.makeText(this, "Google Reader subscriptions imported", Toast.LENGTH_LONG);
		startActivity(new Intent(this, SubscriptionListActivity.class));
	}
}
