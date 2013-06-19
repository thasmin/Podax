package com.axelby.podax.ui;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.androidquery.AQuery;
import com.axelby.podax.BootReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodaxLog;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.axelby.podax.ui.PreferenceListFragment.OnPreferenceAttachedListener;

public class MainActivity extends SherlockFragmentActivity implements OnPreferenceAttachedListener {

	private DrawerLayout _drawerLayout;
	private PodaxDrawerAdapter _drawerAdapter;
	private int _fragmentId;
	List<WeakReference<Fragment>> _savedFragments = new ArrayList<WeakReference<Fragment>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// check if this was opened by android to save an RSS feed
		Intent intent = getIntent();
		if (intent.getDataString() != null && intent.getData().getScheme().equals("http")) {
			ContentValues values = new ContentValues();
			values.put(SubscriptionProvider.COLUMN_URL, intent.getDataString());
			Uri savedSubscription = getContentResolver().insert(SubscriptionProvider.URI, values);
			UpdateService.updateSubscription(this, Integer.valueOf(savedSubscription.getLastPathSegment()));
		}

		// clear RSS error notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);

		BootReceiver.setupAlarms(getApplicationContext());

		// ui initialization
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.app);
		getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activePodcastObserver);
		updateDrawerControls();

		AQuery aq = new AQuery(this);
		_drawerLayout = (DrawerLayout) aq.find(R.id.drawer_layout).getView();
		_drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		ListView drawer = aq.find(R.id.drawer_list).getListView();
		_drawerAdapter = new PodaxDrawerAdapter(this);
		drawer.setAdapter(_drawerAdapter);
		drawer.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				_drawerLayout.closeDrawer(GravityCompat.START);

				_fragmentId = position;

				switch (position) {
					case 1 : replaceFragment(WelcomeFragment.class); break;
					case 2 : replaceFragment(PodcastDetailFragment.class); break;
					case 3 : replaceFragment(QueueFragment.class); break;
					case 4 : replaceFragment(SubscriptionListFragment.class); break;
					case 5 : replaceFragment(SearchFragment.class); break;
					case 7 : askForRSSUrl(); break;
					case 8 : replaceFragment(ITunesPopularListFragment.class); break;
					case 9 : replaceFragment(PodaxPreferenceFragment.class); break;
					case 10: replaceFragment(AboutFragment.class); break;
					case 11: replaceFragment(LogViewerFragment.class); break;
				}
 			}
		});
		aq.id(R.id.pause).clicked(new OnClickListener() {
			@Override
			public void onClick(View view) {
				PlayerService.playstop(MainActivity.this);
			}
		});

		if (intent.hasExtra(Constants.EXTRA_FRAGMENT)) {
			_fragmentId = intent.getIntExtra("fragmentId", 2);
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			if (_fragmentId == 2) {
				ft.add(R.id.fragment, new PodcastDetailFragment());
			} else if (_fragmentId == 4) {
				ft.add(R.id.fragment, new SubscriptionListFragment());
			}
			ft.commit();
		} else if (savedInstanceState == null) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.fragment, new PodcastDetailFragment());
			ft.commit();
			_fragmentId = 2;
		} else {
			_fragmentId = savedInstanceState.getInt("fragmentId");
		}
	}

	protected void askForRSSUrl() {
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
				UpdateService.updateSubscription(MainActivity.this, subscriptionUri);
			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// do nothing
			}
		});
		alert.show();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("fragmentId", _fragmentId);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		getContentResolver().unregisterContentObserver(_activePodcastObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activePodcastObserver);
		Helper.registerMediaButtons(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (!_drawerLayout.isDrawerOpen(GravityCompat.START))
				_drawerLayout.openDrawer(GravityCompat.START);
			else
				_drawerLayout.closeDrawer(GravityCompat.START);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		_savedFragments.add(new WeakReference<Fragment>(fragment));
	}
	
	public void replaceFragment(Class<? extends Fragment> clazz) {
		Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment);
		if (current.getClass().equals(clazz))
			return;
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		for (WeakReference<Fragment> frag : _savedFragments)
			if (frag.get() != null && frag.get().getClass().equals(clazz)) {
				ft.replace(R.id.fragment, frag.get());
				return;
			}

		try {
			ft.replace(R.id.fragment, (Fragment) clazz.getConstructor().newInstance());
		} catch (IllegalArgumentException e) {
			return;
		} catch (InstantiationException e) {
			return;
		} catch (IllegalAccessException e) {
			return;
		} catch (InvocationTargetException e) {
			return;
		} catch (NoSuchMethodException e) {
			return;
		}
		
		ft.addToBackStack(null);
		ft.commit();
	}

	class PodaxDrawerAdapter extends BaseAdapter {
		public String[] _items = {
			"Podax Screens", "Welcome", "Now Playing", "Playlist", "Podcasts", "Search", 
			"Subscribe to Podcast", "Add RSS Feed", "Top iTunes Podcasts",
			"Preferences", 
			"About",
			"Log Viewer",
		};
		private Context _context;

		public PodaxDrawerAdapter(Context context) {
			_context = context;
		}

		@Override
		public int getCount() {
			// log viewer is only available when debugging
			if (PodaxLog.isDebuggable(MainActivity.this))
				return _items.length;
			return _items.length - 1;
		}

		@Override
		public Object getItem(int position) {
			return _items[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO: should reuse convertView if possible
			int layoutId;
			switch (position) {
			case 0:
			case 6:
			case 9:
			case 10:
			case 11:
				layoutId = R.layout.drawer_header_listitem;
				break;
			default:
				layoutId = R.layout.drawer_listitem;
				break;
			}
			TextView tv = (TextView) LayoutInflater.from(_context).inflate(layoutId, null);
			tv.setText(_items[position]);
			return tv;
		}

		@Override
		public boolean isEnabled(int position) {
			return position != 0 && position != 6;
		}
	}

	private ContentObserver _activePodcastObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			updateDrawerControls();
		}

		@Override
		public void onChange(boolean selfChange) {
			this.onChange(selfChange, null);
		}
	};

	private void updateDrawerControls() {
		// we could put the db call in a thread
		AQuery aq = new AQuery(this);
		PlayerStatus status = PlayerStatus.getCurrentState(MainActivity.this);
		aq.id(R.id.pause).image(status.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
		if (status.hasActivePodcast())
			aq.id(R.id.podcast_title).text(status.getTitle());
		else
			aq.id(R.id.podcast_title).text("Queue empty");
	}

	@Override
	public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
		if (root == null)
			return;
	}
}