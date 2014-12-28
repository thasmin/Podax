package com.axelby.podax.ui;

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

import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class PodaxFragmentActivity extends ActionBarActivity {

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
		String fragmentClassName = intent.getStringExtra(Constants.EXTRA_FRAGMENT_CLASSNAME);
		if (fragmentClassName == null || fragmentClassName.equals(""))
			finish();
		Bundle bundle = intent.getBundleExtra(Constants.EXTRA_ARGS);
		createFragment(fragmentClassName, bundle);
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

    void createFragment(String fragmentClassName, Bundle arguments) {
        Fragment fragment = Fragment.instantiate(this, fragmentClassName);
        fragment.setArguments(arguments);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment, fragment);
        ft.commit();
    }
}
