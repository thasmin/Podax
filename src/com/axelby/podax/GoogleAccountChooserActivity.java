package com.axelby.podax;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;

public class GoogleAccountChooserActivity extends ListActivity {
	protected AccountManager _accountManager;
	protected Account[] _accounts;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_accountManager = AccountManager.get(getApplicationContext());
		_accounts = _accountManager.getAccountsByType("com.google");
		String[] names = new String[_accounts.length];
		for (int i = 0; i < _accounts.length; ++i)
			names[i] = _accounts[i].name;			
		this.setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, names));
	}

	@Override
	protected void onListItemClick(ListView l, View v, final int position, long itemId) {
		getListView().setEnabled(false);
		Thread thread = new Thread(new Runnable() {
			public void run() {
				Account account = _accounts[position];
				HttpTransport transport = GoogleTransport.create();
				GoogleHeaders headers = (GoogleHeaders) transport.defaultHeaders;
				headers.setApplicationName("Podax");
				headers.gdataVersion = "2";

				AccountManagerFuture<Bundle> accountManagerFuture = _accountManager.getAuthToken(account, "reader", true, null, null);
				try {
					Bundle authTokenBundle = accountManagerFuture.getResult();
					String authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
					headers.setGoogleLogin(authToken);
					HttpRequest request = transport.buildGetRequest();
					request.url = new GenericUrl("http://www.google.com/reader/api/0/subscription/list");
					InputStream response;
					try {
						response = request.execute().getContent();
					} catch (HttpResponseException ex) {
						_accountManager.invalidateAuthToken("reader", authToken);
						authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
						headers.setGoogleLogin(authToken);
						request = transport.buildPostRequest();
						request.url = new GenericUrl("http://www.google.com/reader/api/0/subscription/list");
						response = request.execute().getContent();
					}

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
							SubscriptionUpdateService.getInstance().updateSubscription(subscription);
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

					// parse the repsonse
					Xml.parse(response, Xml.Encoding.UTF_8, root.getContentHandler());

				} catch (OperationCanceledException e) {
					e.printStackTrace();
				} catch (AuthenticatorException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Toast.makeText(GoogleAccountChooserActivity.this, "Google Reader subscriptions imported", Toast.LENGTH_LONG);
		Intent intent = new Intent();
		intent.setClassName("com.axelby.podax", "com.axelby.podax.SubscriptionListActivity");
		startActivity(intent);
    }
}
