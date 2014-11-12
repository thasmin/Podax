package com.axelby.podax.ui;

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
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
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
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;

public class MainActivity extends ActionBarActivity {

    private ActionBarDrawerToggle _drawerToggle;
    private DrawerLayout _drawerLayout;

    private View _bottom;
    private View _bottombar;
    private ImageButton _play;
    private TextView _episodeTitle;
    private ImageButton _expand;

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

        setContentView(R.layout.app);

        getSupportFragmentManager().beginTransaction().add(R.id.mainlayout, new MainFragment()).commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView drawer = (ListView) findViewById(R.id.drawer);
        PodaxDrawerAdapter _drawerAdapter = new PodaxDrawerAdapter(this);
        drawer.setAdapter(_drawerAdapter);
        drawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                _drawerLayout.closeDrawer(GravityCompat.START);

                Intent activityIntent = new Intent(view.getContext(), PodaxFragmentActivity.class);
                activityIntent.putExtra(Constants.EXTRA_FRAGMENT, id);
                startActivity(activityIntent);
            }
        });

        _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        _drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        _drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        _drawerLayout.setDrawerListener(_drawerToggle);


        // bottom bar controls
        _bottom = findViewById(R.id.bottom);
        _bottombar = findViewById(R.id.bottombar);
        _play = (ImageButton) findViewById(R.id.play);
        _episodeTitle = (TextView) findViewById(R.id.episodeTitle);
        _expand = (ImageButton) findViewById(R.id.expand);
        getSupportFragmentManager().beginTransaction().add(R.id.nowplaying_fragment, new EpisodeDetailFragment()).commit();

        PlayerStatus playerState = PlayerStatus.getCurrentState(this);

        // set proper bottom margin on bottom bar
        ViewTreeObserver.OnPreDrawListener predrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) _bottom.getLayoutParams();
                params.bottomMargin = _bottombar.getHeight() - _bottom.getHeight();
                _bottom.requestLayout();
                _bottom.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        };
        _bottom.getViewTreeObserver().addOnPreDrawListener(predrawListener);

        // handle drag events
        final GestureDetectorCompat _gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            // necessary for any events to fire
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onScroll(MotionEvent e, MotionEvent e2, float velocityX, float velocityY) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) _bottom.getLayoutParams();
                params.bottomMargin -= e2.getY() - e.getY();
                _bottom.requestLayout();
                return true;
            }
            @Override public boolean onFling(MotionEvent e, MotionEvent e2, float velocityX, float velocityY) {
                if (velocityY < 0) {
                    animateBottomBarUp();
                    return true;
                } else if (velocityY > 0) {
                    animateBottomBarDown();
                    return true;
                }
                return false;
            }
        });
        _bottombar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return _gestureDetector.onTouchEvent(motionEvent);
            }
        });

        int playResource = playerState.isPlaying() ? R.drawable.ic_action_pause : R.drawable.ic_action_play;
        _play.setImageResource(playResource);
        _play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
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

        _episodeTitle.setText(playerState.getTitle());

        _expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View parent = (View) _bottom.getParent();
                int[] parentLoc = new int[2];
                parent.getLocationInWindow(parentLoc);
                int[] loc = new int[2];
                _bottom.getLocationInWindow(loc);

                int target = parentLoc[1] + loc[1] - _bottom.getHeight();
                animateBottomBarTo(target);
            }
        });
    }

    private void animateBottomBarUp() { animateBottomBarTo(326); }
    private void animateBottomBarDown() { animateBottomBarTo(-700); }

    private void animateBottomBarTo(final int target) {
        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) _bottom.getLayoutParams();
        if (params.bottomMargin == target)
            return;
        ValueAnimator animator = ValueAnimator.ofInt(params.bottomMargin, target);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                params.bottomMargin = (Integer) valueAnimator.getAnimatedValue();
                _bottom.requestLayout();
            }
        });
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
                _expand.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                _expand.setVisibility(View.VISIBLE);
                _expand.setImageResource(target == -700 ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand);
            }
        });
        animator.start();
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
