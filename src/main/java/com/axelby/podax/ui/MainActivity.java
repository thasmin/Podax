package com.axelby.podax.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.axelby.gpodder.AuthenticatorActivity;
import com.axelby.podax.BootReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.GPodderProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

import javax.annotation.Nonnull;

public class MainActivity extends Activity {

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment _fragment;
        private final Activity _activity;
        private final String _tag;
        private final Class<T> _class;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            _activity = activity;
            _tag = tag;
            _class = clz;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (_fragment == null) {
                _fragment = Fragment.instantiate(_activity, _class.getName());
                ft.add(android.R.id.content, _fragment, _tag);
            } else {
                ft.attach(_fragment);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (_fragment != null) {
                ft.detach(_fragment);
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }

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
                                    //replaceFragment(AboutFragment.class);
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

        // ui initialization
        setContentView(R.layout.app);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            actionBar.addTab(actionBar.newTab()
                            .setText(R.string.playlist)
                            .setTabListener(new TabListener<PlaylistFragment>(
                                    this, "playlist", PlaylistFragment.class))
            );

            actionBar.addTab(actionBar.newTab()
                            .setText(R.string.podcasts)
                            .setTabListener(new TabListener<SubscriptionListFragment>(
                                    this, "podcasts", SubscriptionListFragment.class))
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @Nonnull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.now_playing:
                startActivity(new Intent(this, EpisodeDetailActivity.class));
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    private void handleGPodder() {
        AccountManager am = AccountManager.get(this);
        Account[] gpodder_accounts = am.getAccountsByType(Constants.GPODDER_ACCOUNT_TYPE);
        if (gpodder_accounts == null || gpodder_accounts.length == 0)
            startActivity(new Intent(this, AuthenticatorActivity.class));
        else {
            Toast.makeText(this, "Refreshing from gpodder.net as " + gpodder_accounts[0].name, Toast.LENGTH_SHORT).show();
            ContentResolver.requestSync(gpodder_accounts[0], GPodderProvider.AUTHORITY, new Bundle());
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
    protected void onResume() {
        super.onResume();
        Helper.registerMediaButtons(this);
    }
}