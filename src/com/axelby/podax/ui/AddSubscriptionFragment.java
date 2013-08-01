package com.axelby.podax.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.xml.sax.SAXException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.axelby.podax.Constants;
import com.axelby.podax.OPMLImporter;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class AddSubscriptionFragment extends ListFragment {

	private Account[] _gpodderAccounts;

	private final int ADD_RSS = 0;
	private final int ADD_OPML = 1;
	private final int ADD_GPODDER = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		_gpodderAccounts = AccountManager.get(getActivity()).getAccountsByType("com.axelby.gpodder.account");
		setListAdapter(new ImportSubscriptionAdapter());
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (position == ADD_RSS) {
			AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
			alert.setTitle("Podcast URL");
			alert.setMessage("Type the URL of the podcast RSS");
			final EditText input = new EditText(getActivity());
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
					Uri subscriptionUri = getActivity().getContentResolver().insert(SubscriptionProvider.URI, values);
					UpdateService.updateSubscription(getActivity(), subscriptionUri);
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

		if (position == ADD_OPML) {
			FileFilter fileFilter = new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().equals("podcasts.opml");
				}
			};
			File externalStorageDir = Environment.getExternalStorageDirectory();
			File[] opmlFiles = externalStorageDir.listFiles(fileFilter);
			if (opmlFiles!= null && opmlFiles.length == 0) {
				String message = "The OPML file must be at " + externalStorageDir.getAbsolutePath() + "/podcasts.opml.";
				Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
				return;
			}

			try {
				if (getActivity() == null)
					return;
				int newSubscriptions = OPMLImporter.read(getActivity(), opmlFiles[0]);
				String message = "Found " + newSubscriptions + " subscriptions.";
				Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
				getFragmentManager().popBackStack();

				Intent intent = new Intent(getActivity(), MainActivity.class);
				intent.putExtra(Constants.EXTRA_FRAGMENT, 4);
				getActivity().startActivity(intent);
			} catch (IOException e) {
				String message = "There was an error while reading the OPML file.";
				Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
			} catch (SAXException e) {
				String message = "The podcasts.opml file is not valid OPML.";
				Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
			}
			return;
		}

		if (position == ADD_GPODDER) {
			if (_gpodderAccounts.length > 0) {
				return;
			}
			Intent intent = new Intent(getActivity(), com.axelby.gpodder.AuthenticatorActivity.class);
			startActivity(intent);
		}

		super.onListItemClick(l, v, position, id);
	}


	public class ImportSubscriptionAdapter extends BaseAdapter {
		LayoutInflater _inflater;

		public ImportSubscriptionAdapter() {
			_inflater = (LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return 3;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == ADD_RSS) {
				TextView view = (TextView) _inflater.inflate(R.layout.list_item, parent, false);
				view.setText(R.string.add_rss_feed);
				return view;
			}

			if (position == ADD_OPML) {
				TextView view = (TextView) _inflater.inflate(R.layout.list_item, parent, false);
				view.setText(R.string.add_from_opml_file);
				return view;
			}

			if (position == ADD_GPODDER) {
				View view = _inflater.inflate(R.layout.subscription_list_item, parent, false);

				TextView text = (TextView)view.findViewById(R.id.text);
				ImageView thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
				thumbnail.setImageResource(R.drawable.mygpo);

				if (_gpodderAccounts.length == 0)
					text.setText("Link a GPodder account");
				else
					text.setText("Linked to " + _gpodderAccounts[0].name);
				return view;
			}
			
			return null;
		}
	}
}
