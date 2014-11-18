package com.axelby.podax.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

import com.axelby.gpodder.AuthenticatorActivity;
import com.axelby.podax.Constants;
import com.axelby.podax.GPodderProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class PodaxFragmentActivity extends ActionBarActivity {

    public final static long FRAGMENT_GPODDER = 0;
    public final static long FRAGMENT_STATS = 1;
    public final static long FRAGMENT_PREFERENCES = 2;
    public final static long FRAGMENT_ABOUT = 3;
    public final static long FRAGMENT_LOG_VIEWER = 4;
    public final static long FRAGMENT_WELCOME = 5;

    public static Intent createIntent(Context context, long fragmentId) {
        Intent intent = new Intent(context, PodaxFragmentActivity.class);
        intent.putExtra(Constants.EXTRA_FRAGMENT, fragmentId);
        return intent;
    }

    public static Intent createIntent(Context context, Class fragmentClass, String extraId, long id) {
        Intent intent = new Intent(context, PodaxFragmentActivity.class);
        intent.putExtra(Constants.EXTRA_FRAGMENT_CLASSNAME, fragmentClass.getCanonicalName());
        Bundle args = new Bundle(1);
        args.putLong(extraId, id);
        intent.putExtra(Constants.EXTRA_ARGS, args);
        return intent;
    }

    public static Intent createIntent(Context context, Class fragmentClass, Bundle args) {
        Intent intent = new Intent(context, PodaxFragmentActivity.class);
        intent.putExtra(Constants.EXTRA_FRAGMENT_CLASSNAME, fragmentClass.getCanonicalName());
        intent.putExtra(Constants.EXTRA_ARGS, args);
        return intent;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_general);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
		    actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        long fragmentCode = intent.getLongExtra(Constants.EXTRA_FRAGMENT, -1);
        if (fragmentCode == FRAGMENT_GPODDER)
            handleGPodder();
        else if (fragmentCode == FRAGMENT_STATS)
            createFragment(StatsFragment.class, null);
        else if (fragmentCode == FRAGMENT_PREFERENCES)
            createFragment(PodaxPreferenceFragment.class, null);
        else if (fragmentCode == FRAGMENT_ABOUT)
            createFragment(AboutFragment.class, null);
        else if (fragmentCode == FRAGMENT_LOG_VIEWER)
            createFragment(LogViewerFragment.class, null);
        else if (fragmentCode == FRAGMENT_WELCOME)
            createFragment(WelcomeFragment.class, null);
        else {
            String fragmentClassName = intent.getStringExtra(Constants.EXTRA_FRAGMENT_CLASSNAME);
            if (fragmentClassName == null || fragmentClassName.equals(""))
                finish();
            Bundle bundle = intent.getBundleExtra(Constants.EXTRA_ARGS);
            createFragment(fragmentClassName, bundle);
        }
	}

	@Override
	protected void onResume() {
		super.onResume();

		Helper.registerMediaButtons(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

    protected Fragment createFragment(Class<?> fragmentClass, Bundle arguments) {
        return createFragment(fragmentClass.getCanonicalName(), arguments);
    }

    protected Fragment createFragment(String fragmentClassName, Bundle arguments) {
        Fragment fragment = Fragment.instantiate(this, fragmentClassName);
        fragment.setArguments(arguments);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment, fragment);
        ft.commit();
        return fragment;
    }

    private void handleGPodder() {
        AccountManager am = AccountManager.get(this);
        Account[] gpodder_accounts = am.getAccountsByType(Constants.GPODDER_ACCOUNT_TYPE);
        if (gpodder_accounts == null || gpodder_accounts.length == 0) {
            finish();
            startActivity(new Intent(this, AuthenticatorActivity.class));
        } else {
            Toast.makeText(this, "Refreshing from gpodder.net as " + gpodder_accounts[0].name, Toast.LENGTH_SHORT).show();
            ContentResolver.requestSync(gpodder_accounts[0], GPodderProvider.AUTHORITY, new Bundle());
        }
    }

}
