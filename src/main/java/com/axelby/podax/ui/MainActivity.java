package com.axelby.podax.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.axelby.podax.BootReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodaxLog;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class MainActivity extends Activity {

    private ActionBarDrawerToggle _drawerToggle;
    private DrawerLayout _drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if this was opened by android to save an RSS feed
        final Intent intent = getIntent();
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
                                    startActivity(PodaxFragmentActivity.createIntent(MainActivity.this, PodaxFragmentActivity.FRAGMENT_ABOUT));
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

        @SuppressLint("AppCompatMethod") ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        setContentView(R.layout.app);

        getFragmentManager().beginTransaction().add(R.id.mainlayout, new MainFragment()).commit();

        ListView drawer = (ListView) findViewById(R.id.drawer);
        PodaxDrawerAdapter _drawerAdapter = new PodaxDrawerAdapter(this);
        drawer.setAdapter(_drawerAdapter);
        drawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                _drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(PodaxFragmentActivity.createIntent(view.getContext(), id));
            }
        });

        _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        _drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        _drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, R.drawable.ic_drawer,
                R.string.open_drawer, R.string.close_drawer);
        _drawerLayout.setDrawerListener(_drawerToggle);
    }

    private boolean isPlayerServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (PlayerService.class.getName().equals(service.service.getClassName()))
                return true;
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Helper.registerMediaButtons(this);

        if (getContentResolver().query(SubscriptionProvider.URI, null, null, null, null).getCount() == 0) {
            Helper.changeFragment(this, AddSubscriptionFragment.class, null);
        }
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

    class PodaxDrawerAdapter extends BaseAdapter {
        Item _items[] = {
                new Item(PodaxFragmentActivity.FRAGMENT_GPODDER, R.string.gpodder_sync, R.drawable.ic_menu_mygpo),
                new Item(PodaxFragmentActivity.FRAGMENT_STATS, R.string.stats, R.drawable.ic_menu_settings),
                new Item(PodaxFragmentActivity.FRAGMENT_PREFERENCES, R.string.preferences, R.drawable.ic_menu_configuration),
                new Item(PodaxFragmentActivity.FRAGMENT_ABOUT, R.string.about, R.drawable.ic_menu_podax),
                new Item(PodaxFragmentActivity.FRAGMENT_LOG_VIEWER, R.string.log_viewer, android.R.drawable.ic_menu_info_details),
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
            return _items[position].id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int layoutId = R.layout.drawer_listitem;
            if (convertView == null)
                convertView = LayoutInflater.from(_context).inflate(layoutId, null);
            if (convertView == null)
                return null;

            TextView tv = (TextView) convertView;
            final Item item = _items[position];
            tv.setText(item.label);
            tv.setCompoundDrawablesWithIntrinsicBounds(item.drawable, 0, 0, 0);
            return tv;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        class Item {
            long id;
            String label;
            int drawable;

            public Item(long id, int labelId, int drawableId) {
                this.id = id;
                this.drawable = drawableId;
                this.label = MainActivity.this.getResources().getString(labelId);
            }
        }
    }
}