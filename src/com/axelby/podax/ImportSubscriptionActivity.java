package com.axelby.podax;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ImportSubscriptionActivity extends ListActivity {

	private AccountManager _accountManager;
	private Account[] _googleAccounts;
	private Account _chosenAccount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscription_list);

		_accountManager = AccountManager.get(getApplicationContext());
		_accountManager.getAccountsByTypeAndFeatures("com.google", new String[] { "service_reader" },
				new AccountManagerCallback<Account[]>() {
					public void run(AccountManagerFuture<Account[]> future) {
						try {
							_googleAccounts = future.getResult();
							ImportSubscriptionActivity.this
									.setListAdapter(new ImportSubscriptionAdapter());
						} catch (OperationCanceledException e) {
							Log.e("Podax", "Operation Canceled", e);
						} catch (IOException e) {
							Log.e("Podax", "IOException", e);
						} catch (AuthenticatorException e) {
							Log.e("Podax", "Authentication Failed", e);
						}
					}
			}, null);

		//registerForContextMenu(getListView());
		
		PlayerActivity.injectPlayerFooter(this);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		if (position > 1) {
			_chosenAccount = _googleAccounts[position - 2];
			getAuthToken();
		}
	}


	public class ImportSubscriptionAdapter extends BaseAdapter {
		LayoutInflater _inflater;
		
		public ImportSubscriptionAdapter() {
			_inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
		}
		
		public int getCount() {
			return _googleAccounts.length == 0 ? 1 : _googleAccounts.length + 2;
		}

		public Object getItem(int position) {
			if (position < 2)
				return null;
			return _googleAccounts[position - 2];
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				TextView view = (TextView) _inflater.inflate(R.layout.list_item, null);
				view.setText("Import from OPML file");
				return view;
			}
			if (position == 1) {
				TextView view = new TextView(ImportSubscriptionActivity.this);
				view.setTextAppearance(ImportSubscriptionActivity.this, android.R.style.TextAppearance_Medium);
				view.setBackgroundDrawable(getResources().getDrawable(R.drawable.back));
				view.setText("Google Reader Accounts");
				return view;
			}

			TextView view = (TextView) _inflater.inflate(R.layout.list_item, null);
			view.setText(_googleAccounts[position - 2].name);
			return view;
		}

		public boolean areAllItemsEnabled() {
			return false;
		}

		public boolean isEnabled(int position) {
			return position != 1;
		}
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
			final DBAdapter adapter = DBAdapter.getInstance(this);

			object.setElementListener(new ElementListener() {
				public void start(Attributes attrs) {
					id[0] = "";
					title[0] = "";
				}
				public void end() {
					Subscription subscription = adapter.addSubscription(id[0], title[0]);
					UpdateService.updateSubscription(ImportSubscriptionActivity.this, subscription);
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
		finish();
		startActivity(new Intent(this, SubscriptionListActivity.class));
	}

}
