package com.axelby.podax.ui;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.axelby.podax.AppFlow;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodaxLog;
import com.axelby.podax.R;
import com.axelby.podax.model.Episodes;
import com.axelby.podax.model.SubscriptionEditor;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

public class MainActivity extends RxAppCompatActivity {

    private ActionBarDrawerToggle _drawerToggle;
    private DrawerLayout _drawerLayout;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if this was opened by android to save an RSS feed
        Intent intent = getIntent();
        if (intent.getDataString() != null && intent.getData().getScheme().equals("http"))
            SubscriptionEditor.addNewSubscription(this, intent.getDataString());

        // clear RSS error notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);
		notificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD_ERROR);

        if (!isPlayerServiceRunning())
            PlayerStatus.updateState(this, PlayerStatus.PlayerStates.STOPPED);

        // release notes dialog
		int versionCode = Helper.getVersionCode(this);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int lastReleaseNoteDialog = preferences.getInt("lastReleaseNoteDialog", 0);
		if (lastReleaseNoteDialog > 0 && lastReleaseNoteDialog < versionCode) {
			showReleaseNotesDialog();
			preferences.edit().putInt("lastReleaseNoteDialog", versionCode).apply();
		}

		setupUI();

		showLatestActivityIfNewEpisodes();
    }

	private void showReleaseNotesDialog() {
		new AlertDialog.Builder(this)
			.setTitle(R.string.release_notes)
			.setMessage(R.string.release_notes_detailed)
			.setPositiveButton(R.string.view_release_notes, (dialogInterface, i) -> {
				AppFlow.get(this).onRequestViewReleaseNotes();
			})
			.setNegativeButton(R.string.no_thanks, (dialogInterface, i) -> { })
			.create()
			.show();
	}

	private void showLatestActivityIfNewEpisodes() {
		SharedPreferences prefs = getSharedPreferences("latest_activity", Context.MODE_PRIVATE);

		boolean showAutomatically = prefs.getBoolean("automatic_show", true);
		long lastActivityCheck = prefs.getLong("last_check", 0);
		if (showAutomatically && Episodes.isLastActivityAfter(lastActivityCheck))
			AppFlow.get(this).displayLatestActivity();
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
		navMenu.setNavigationItemSelectedListener(_handleDrawerItem);

		if (!PodaxLog.isDebuggable(this))
			navMenu.findViewById(R.id.log_viewer).setVisibility(View.GONE);

		_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		_drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		_drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, toolbar,
				R.string.open_drawer, R.string.close_drawer);
		_drawerLayout.addDrawerListener(_drawerToggle);

		// start by acting on first item in nav menu
		_handleDrawerItem.onNavigationItemSelected(navMenu.getMenu().getItem(0));
	}

	@NonNull
	private NavigationView.OnNavigationItemSelectedListener _handleDrawerItem = new NavigationView.OnNavigationItemSelectedListener() {
		@Override
		public boolean onNavigationItemSelected(MenuItem menuItem) {
			_drawerLayout.closeDrawer(GravityCompat.START);
			return AppFlow.get(MainActivity.this).onMainMenuItem(menuItem.getItemId());
		}
	};

	public void showMainFragment(CharSequence title, Fragment fragment) {
		if (getSupportActionBar() != null)
			getSupportActionBar().setTitle(title);

		FragmentTransaction trans = getFragmentManager().beginTransaction();
		trans.replace(R.id.fragment, fragment);
		trans.commit();
	}

	public boolean hasDetailFragment() {
		// TODO: determine whether the layout has a detail fragment
		return false;
	}

	public void showDetailFragment(Fragment fragment) {
		// TODO: determine whether the layout has a detail fragment and put the fragment there
		FragmentTransaction trans = getFragmentManager().beginTransaction();
		trans.replace(R.id.fragment, fragment);
		trans.commit();
	}

	@Override
	public void onBackPressed() {
		AppFlow.get(this).goBack();
	}

    private boolean isPlayerServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (PlayerService.class.getName().equals(service.service.getClassName()))
                return true;
        return false;
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

}
