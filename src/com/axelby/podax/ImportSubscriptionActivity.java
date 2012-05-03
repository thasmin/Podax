package com.axelby.podax;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.xml.sax.SAXException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ImportSubscriptionActivity extends ListActivity {

	private AccountManager _accountManager;
	private Account[] _gpodderAccounts;
	private Account[] _googleAccounts = { };
	private Account _chosenAccount;

	private final int GOOGLE_ACCOUNT_START = 5;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscription_list);

		_accountManager = AccountManager.get(getApplicationContext());
		_gpodderAccounts = _accountManager.getAccountsByType("com.axelby.gpodder.account");
		_accountManager.getAccountsByTypeAndFeatures("com.google", new String[] { "service_reader" },
				new AccountManagerCallback<Account[]>() {
					public void run(AccountManagerFuture<Account[]> future) {
						try {
							_googleAccounts = future.getResult();
						} catch (OperationCanceledException e) {
							Log.e("Podax", "Operation Canceled", e);
						} catch (IOException e) {
							Log.e("Podax", "IOException", e);
						} catch (AuthenticatorException e) {
							// no authenticator registered
						} finally {
							ImportSubscriptionActivity.this
									.setListAdapter(new ImportSubscriptionAdapter());
						}
					}
			}, null);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position == 0) {
    		AlertDialog.Builder alert = new AlertDialog.Builder(this);
    		alert.setTitle("Podcast URL");
    		alert.setMessage("Type the URL of the podcast RSS");
    		final EditText input = new EditText(this);
    		//input.setText("http://blog.axelby.com/podcast.xml");
    		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    		alert.setView(input);
    		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String subscriptionUrl = input.getText().toString();
					if (!subscriptionUrl.contains("://"))
						subscriptionUrl = "http://" + subscriptionUrl;
					ContentValues values = new ContentValues();
					values.put(SubscriptionProvider.COLUMN_URL, subscriptionUrl);
					values.put(SubscriptionProvider.COLUMN_TITLE, subscriptionUrl);
					Uri subscriptionUri = getContentResolver().insert(SubscriptionProvider.URI, values);
					UpdateService.updateSubscription(ImportSubscriptionActivity.this, subscriptionUri);
				}
			});
    		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// do nothing
				}
			});
    		alert.show();
    		return;
    	}

		if (position == 1) {
			FileFilter fileFilter = new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().equals("podcasts.opml");
				}
			};
			File externalStorageDir = Environment.getExternalStorageDirectory();
			File[] opmlFiles = externalStorageDir.listFiles(fileFilter);
			if (opmlFiles.length == 0) {
				String message = "The OPML file must be at " + externalStorageDir.getAbsolutePath() + "/podcasts.opml.";
				Toast.makeText(this, message, Toast.LENGTH_LONG).show();
				return;
			}

			try {
				int newSubscriptions = OPMLImporter.read(this, opmlFiles[0]);
				String message = "Found " + newSubscriptions + " subscriptions.";
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
				finish();
				startActivity(new Intent(this, SubscriptionListActivity.class));
			} catch (IOException e) {
				String message = "There was an error while reading the OPML file.";
				Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			} catch (SAXException e) {
				String message = "The podcasts.opml file is not valid OPML.";
				Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			}
			return;
		}

		if (position == 2) {
			startActivity(new Intent(this, DiscoverActivity.class));
			return;
		}

		if (position == 3) {
			if (_gpodderAccounts.length > 0) {
				return;
			} else if (!Helper.isGPodderInstalled(this)) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.axelby.gpodder"));
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			} else {
				Intent intent = new Intent();
				intent.setClassName("com.axelby.gpodder", "com.axelby.gpodder.AuthenticatorActivity");
				startActivity(intent);
			}
		}

		if (position >= GOOGLE_ACCOUNT_START) {
			_chosenAccount = _googleAccounts[position - GOOGLE_ACCOUNT_START];
			getAuthToken();
			return;
		}

		super.onListItemClick(l, v, position, id);
	}


	public class ImportSubscriptionAdapter extends BaseAdapter {
		LayoutInflater _inflater;
		
		public ImportSubscriptionAdapter() {
			_inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
		}
		
		public int getCount() {
			return _googleAccounts.length == 0 ? 
						GOOGLE_ACCOUNT_START - 1 :
						_googleAccounts.length + GOOGLE_ACCOUNT_START;
		}

		public Object getItem(int position) {
			if (position < GOOGLE_ACCOUNT_START)
				return null;
			return _googleAccounts[position - GOOGLE_ACCOUNT_START];
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				TextView view = (TextView) _inflater.inflate(R.layout.list_item, null);
				view.setText(R.string.add_rss_feed);
				return view;
			}

			if (position == 1) {
				TextView view = (TextView) _inflater.inflate(R.layout.list_item, null);
				view.setText(R.string.add_from_opml_file);
				return view;
			}

			if (position == 2) {
				TextView view = (TextView) _inflater.inflate(R.layout.list_item, null);
				view.setText(R.string.discover_subscriptions);
				return view;
			}

			if (position == 3) {
				View view = _inflater.inflate(R.layout.subscription_list_item, null);

				TextView text = (TextView)view.findViewById(R.id.text);
				ImageView thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
				thumbnail.setImageResource(R.drawable.mygpo);

				if (_gpodderAccounts.length == 0)
					text.setText("Link a GPodder account");
				else
					text.setText("Linked to " + _gpodderAccounts[0].name);
				return view;
			}

			if (position == GOOGLE_ACCOUNT_START - 1) {
				TextView view = new TextView(ImportSubscriptionActivity.this);
				view.setTextAppearance(ImportSubscriptionActivity.this, android.R.style.TextAppearance_Medium);
				view.setBackgroundDrawable(getResources().getDrawable(R.drawable.back));
				view.setText("Google Reader Accounts");
				return view;
			}

			TextView view = (TextView) _inflater.inflate(R.layout.list_item, null);
			view.setText(_googleAccounts[position - GOOGLE_ACCOUNT_START].name);
			return view;
		}

		public boolean areAllItemsEnabled() {
			return false;
		}

		public boolean isEnabled(int position) {
			return position != GOOGLE_ACCOUNT_START - 1;
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

					Activity activity = ImportSubscriptionActivity.this;
					String authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
					GoogleReaderImporter.doImport(activity, authToken);
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

}
