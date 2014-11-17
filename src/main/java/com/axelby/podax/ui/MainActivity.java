package com.axelby.podax.ui;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;

public class MainActivity extends ActionBarActivity {

    private ActionBarDrawerToggle _drawerToggle;
    private DrawerLayout _drawerLayout;

    private View _main;
    private View _bottombar;
    private View _nowplaying_fragment;
    private ImageButton _play;
    private TextView _episodeTitle;
    private ImageButton _expand;

    // handle drag events on bottom bar
    private GestureDetectorCompat _bottomGestureDetector;
    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
       // necessary for any events to fire
        @Override public boolean onDown(MotionEvent e) { return true; }
        @Override public boolean onScroll(MotionEvent e, MotionEvent e2, float velocityX, float velocityY) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) _bottombar.getLayoutParams();
            params.bottomMargin -= e2.getY() - e.getY();
            _bottombar.requestLayout();

            params = (ViewGroup.MarginLayoutParams) _nowplaying_fragment.getLayoutParams();
            params.topMargin += e2.getY() - e.getY();
            _nowplaying_fragment.requestLayout();

            return true;
        }
        @Override public boolean onFling(MotionEvent e, MotionEvent e2, float velocityX, float velocityY) {
            float totalVelocityY = e2.getY() - e.getY();
            if (totalVelocityY < 16) {
                animateBottomBarUp();
                return true;
            } else if (totalVelocityY > 16) {
                animateBottomBarDown();
                return true;
            }
            return false;
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
                startActivity(PodaxFragmentActivity.createIntent(view.getContext(), id));
            }
        });

        _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        _drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        _drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        _drawerLayout.setDrawerListener(_drawerToggle);


        // bottom bar controls
        _main = findViewById(R.id.main);
        _bottombar = findViewById(R.id.bottombar);
        _nowplaying_fragment = findViewById(R.id.nowplaying_fragment);
        _play = (ImageButton) findViewById(R.id.play);
        _episodeTitle = (TextView) findViewById(R.id.episodeTitle);
        _expand = (ImageButton) findViewById(R.id.expand);
        getSupportFragmentManager().beginTransaction().add(R.id.nowplaying_fragment, new EpisodeDetailFragment()).commit();

        PlayerStatus playerState = PlayerStatus.getCurrentState(this);

        _bottomGestureDetector = new GestureDetectorCompat(this, gestureListener);
        initializeBottom(playerState);
    }

    private void initializeBottom(PlayerStatus playerState) {
        if (playerState.hasActiveEpisode()) {
            _bottombar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return _bottomGestureDetector.onTouchEvent(motionEvent);
                }
            });

            int playResource = playerState.isPlaying() ? R.drawable.ic_action_pause : R.drawable.ic_action_play;
            _play.setVisibility(View.VISIBLE);
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

            _episodeTitle.setText(playerState.getTitle());

            _expand.setVisibility(View.VISIBLE);
            _expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    animateBottomBarToggle();
                }
            });
        } else {
            _play.setVisibility(View.INVISIBLE);
            _episodeTitle.setText(R.string.playlist_empty);
            _expand.setVisibility(View.INVISIBLE);
        }
    }

    private void animateBottomBarToggle() {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) _bottombar.getLayoutParams();
        if (params.bottomMargin == 0)
            animateBottomBarUp();
        else
            animateBottomBarDown();
    }
    private void animateBottomBarUp() { animateBottomBarTo(_main.getHeight() - _bottombar.getHeight()); }
    private void animateBottomBarDown() { animateBottomBarTo(0); }

    private void animateBottomBarTo(final int target) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) _bottombar.getLayoutParams();
        if (params.bottomMargin == target)
            return;

        ValueAnimator animator = ValueAnimator.ofInt(params.bottomMargin, target);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.MarginLayoutParams barParams = (ViewGroup.MarginLayoutParams) _bottombar.getLayoutParams();
                barParams.bottomMargin = (Integer) valueAnimator.getAnimatedValue();
                _bottombar.requestLayout();

                ViewGroup.MarginLayoutParams fragParams = (ViewGroup.MarginLayoutParams) _nowplaying_fragment.getLayoutParams();
                fragParams.topMargin = (Integer) valueAnimator.getAnimatedValue() * -1;
                _nowplaying_fragment.requestLayout();
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
                _expand.setImageResource(target == 0 ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand);
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
