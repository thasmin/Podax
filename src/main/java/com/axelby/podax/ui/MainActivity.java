package com.axelby.podax.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import com.axelby.gpodder.AuthenticatorActivity;
import com.axelby.podax.BootReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.GPodderProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodaxLog;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;

public class MainActivity extends AppCompatActivity {

    private ActionBarDrawerToggle _drawerToggle;
    private DrawerLayout _drawerLayout;

    private int _fragment;

	private View _progressbg;
	private View _progressline;
    private ImageButton _play;
    private TextView _episodeTitle;
    private ImageButton _expand;

    private final ContentObserver _activeEpisodeObserver = new ContentObserver(new Handler()) {
        @Override public void onChange(boolean selfChange) { onChange(selfChange, null); }
        @Override public void onChange(boolean selfChange, Uri uri) {
            initializeBottom(PlayerStatus.getCurrentState(MainActivity.this));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if this was opened by android to save an RSS feed
        Intent intent = getIntent();
        if (intent.getDataString() != null && intent.getData().getScheme().equals("http"))
            SubscriptionProvider.addNewSubscription(this, intent.getDataString());

        // clear RSS error notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);
		notificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD_ERROR);

        BootReceiver.setupAlarms(getApplicationContext());

        if (!isPlayerServiceRunning())
            PlayerStatus.updateState(this, PlayerStatus.PlayerStates.STOPPED);

        // release notes dialog
        try {
            PackageManager packageManager = getApplication().getPackageManager();
            if (packageManager != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                int lastReleaseNoteDialog = preferences.getInt("lastReleaseNoteDialog", 0);
                int versionCode = packageManager.getPackageInfo(getPackageName(), 0).versionCode;
                if (lastReleaseNoteDialog < versionCode) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.release_notes)
                            .setMessage(R.string.release_notes_detailed)
                            .setPositiveButton(R.string.view_release_notes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(PodaxFragmentActivity.createIntent(MainActivity.this, AboutFragment.class, null));
                                }
                            })
                            .setNegativeButton(R.string.no_thanks, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            })
                            .create()
                            .show();
                    preferences.edit().putInt("lastReleaseNoteDialog", versionCode).apply();
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

		showLatestActivityIfNewEpisodes();

		setupUI();
    }

	private void showLatestActivityIfNewEpisodes() {
		SharedPreferences prefs = getSharedPreferences("latest_activity", Context.MODE_PRIVATE);

		boolean showAutomatically = prefs.getBoolean("automatic_show", true);
		if (!showAutomatically)
			return;

		long lastActivityCheck = prefs.getLong("last_check", 0);
		Cursor c = getContentResolver().query(EpisodeProvider.LATEST_ACTIVITY_URI,
				null, EpisodeProvider.COLUMN_PUB_DATE + ">?",
				new String[] { String.valueOf(lastActivityCheck) }, null);
		if (c != null) {
			if (c.getCount() > 0) {
				startActivity(PodaxFragmentActivity.createIntent(this, LatestActivityFragment.class, null));
			}
			c.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.dashboard, menu);

		SearchManager searchManager = (SearchManager) MainActivity.this.getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
		ComponentName cn = new ComponentName(this, SearchActivity.class);
		searchView.setSearchableInfo(searchManager.getSearchableInfo(cn));

		return super.onCreateOptionsMenu(menu);
	}

	private void setupUI() {
		setContentView(R.layout.app);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		NavigationView navMenu = (NavigationView) findViewById(R.id.navmenu);
		navMenu.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem menuItem) {
				_drawerLayout.closeDrawer(GravityCompat.START);

				switch (menuItem.getItemId()) {
					case R.id.latest_activity:
						startFragmentActivity(LatestActivityFragment.class);
						return true;
					case R.id.weekly_planner:
						startFragmentActivity(WeeklyPlannerFragment.class);
						return true;
					case R.id.finished_episodes:
						startFragmentActivity(FinishedEpisodeFragment.class);
						return true;
					case R.id.add_subscription:
						startFragmentActivity(AddSubscriptionFragment.class);
						return true;
					case R.id.gpodder:
						handleGPodder();
						return true;
					case R.id.stats:
						startFragmentActivity(StatsFragment.class);
						return true;
					case R.id.preferences:
						startFragmentActivity(PreferenceFragment.class);
						return true;
					case R.id.about:
						startFragmentActivity(AboutFragment.class);
						return true;
					case R.id.log_viewer:
						startFragmentActivity(LogViewerFragment.class);
						return true;
				}
				return false;
			}
		});

		if (!PodaxLog.isDebuggable(this))
			navMenu.findViewById(R.id.log_viewer).setVisibility(View.GONE);

		_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		_drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		_drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, toolbar,
				R.string.open_drawer, R.string.close_drawer);
		_drawerLayout.setDrawerListener(_drawerToggle);

		// main section
		final TextView playlistText = (TextView) findViewById(R.id.toolbar_playlist_btn);
		final TextView subscriptionsText = (TextView) findViewById(R.id.toolbar_subscriptions_btn);
		playlistText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (_fragment == 0)
					return;
				_fragment = 0;
				setTabActive(playlistText);
				setTabInactive(subscriptionsText);
				getFragmentManager().beginTransaction().replace(R.id.fragment, new PlaylistFragment()).commit();
			}
		});
		subscriptionsText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (_fragment == 1)
					return;
				_fragment = 1;
				setTabActive(subscriptionsText);
				setTabInactive(playlistText);
				getFragmentManager().beginTransaction().replace(R.id.fragment, new SubscriptionListFragment()).commit();
			}
		});

		FragmentManager fragmentManager = getFragmentManager();
		if (_fragment == 0)
			fragmentManager.beginTransaction().replace(R.id.fragment, new PlaylistFragment()).commit();
		else if (_fragment == 1)
			fragmentManager.beginTransaction().replace(R.id.fragment, new SubscriptionListFragment()).commit();

		// bottom bar controls
		_progressbg = findViewById(R.id.progressbg);
		_progressline = findViewById(R.id.progressline);
		_play = (ImageButton) findViewById(R.id.play);
		_episodeTitle = (TextView) findViewById(R.id.episodeTitle);
		_expand = (ImageButton) findViewById(R.id.expand);
		initializeBottom(PlayerStatus.getCurrentState(this));
	}

	private void startFragmentActivity(Class<? extends Fragment> fragmentClass) {
		startActivity(PodaxFragmentActivity.createIntent(MainActivity.this, fragmentClass, null));

	}

	private void decorateTab(TextView tab, @ColorRes int textColor, @DrawableRes int background) {
		int pL = tab.getPaddingLeft();
		int pT = tab.getPaddingTop();
		int pR = tab.getPaddingRight();
		int pB = tab.getPaddingBottom();
		tab.setTextColor(getResources().getColor(textColor));
		tab.setBackgroundResource(background);
		tab.setPadding(pL, pT, pR, pB);
	}

	private void setTabActive(TextView tab) {
		decorateTab(tab, R.color.white, R.drawable.main_tab);
	}

	private void setTabInactive(TextView tab) {
		decorateTab(tab, R.color.dimTextOnPrimary, 0);
	}

	private void initializeBottom(PlayerStatus playerState) {
        if (playerState.hasActiveEpisode()) {
			Point screenSize = new Point();
			getWindowManager().getDefaultDisplay().getSize(screenSize);
			float progress = 1.0f * playerState.getPosition() / playerState.getDuration();
			int widthPx = (int) (screenSize.x * progress);

			_progressbg.setVisibility(View.VISIBLE);
			ViewGroup.MarginLayoutParams bgParams = (ViewGroup.MarginLayoutParams) _progressbg.getLayoutParams();
			bgParams.setMargins(widthPx, 0, 0, 0);
			_progressbg.setLayoutParams(bgParams);

			_progressline.setVisibility(View.VISIBLE);
			ViewGroup.MarginLayoutParams lineParams = (ViewGroup.MarginLayoutParams) _progressline.getLayoutParams();
			lineParams.setMargins(0, 0, screenSize.x - widthPx, 0);
			_progressline.setLayoutParams(lineParams);

           int playResource = playerState.isPlaying() ? R.drawable.ic_action_pause : R.drawable.ic_action_play;
            _play.setImageResource(playResource);
            _play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Context context = MainActivity.this;
                    PlayerStatus playerState = PlayerStatus.getCurrentState(context);
                    if (playerState.isPlaying()) {
                        _play.setImageResource(R.drawable.ic_action_play);
                        PlayerService.stop(context);
                    } else {
                        _play.setImageResource(R.drawable.ic_action_pause);
                        PlayerService.play(context);
                    }
                }
            });

			View.OnClickListener displayCurrentEpisode = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					startActivity(PodaxFragmentActivity.createIntent(MainActivity.this, EpisodeDetailFragment.class, null));
				}
			};

            _episodeTitle.setText(playerState.getTitle());
			_episodeTitle.setOnClickListener(displayCurrentEpisode);

            _expand.setImageResource(R.drawable.ic_action_collapse);
			_expand.setOnClickListener(displayCurrentEpisode);
        } else {
			_progressbg.setVisibility(View.GONE);
			_progressline.setVisibility(View.GONE);
            _play.setImageDrawable(null);
			_play.setOnClickListener(null);
            _episodeTitle.setText(R.string.playlist_empty);
            _expand.setImageDrawable(null);
			_expand.setOnClickListener(null);
        }
    }

    private boolean isPlayerServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (PlayerService.class.getName().equals(service.service.getClassName()))
                return true;
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(_activeEpisodeObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Helper.registerMediaButtons(this);

        if (getContentResolver().query(SubscriptionProvider.URI, null, null, null, null).getCount() == 0)
            startActivity(PodaxFragmentActivity.createIntent(this, AddSubscriptionFragment.class, null));

        getContentResolver().registerContentObserver(EpisodeProvider.ACTIVE_EPISODE_URI, false, _activeEpisodeObserver);
        initializeBottom(PlayerStatus.getCurrentState(this));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        _drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        _drawerToggle.syncState();
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		return _drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
	}

	private void handleGPodder() {
		AccountManager am = AccountManager.get(this);
		Account[] gpodder_accounts = am.getAccountsByType(Constants.GPODDER_ACCOUNT_TYPE);
		if (gpodder_accounts == null || gpodder_accounts.length == 0) {
			startActivity(new Intent(this, AuthenticatorActivity.class));
		} else {
			Snackbar.make(findViewById(R.id.main), "Refreshing from gpodder.net as " + gpodder_accounts[0].name, Snackbar.LENGTH_SHORT).show();
			ContentResolver.requestSync(gpodder_accounts[0], GPodderProvider.AUTHORITY, new Bundle());
		}
	}

}
