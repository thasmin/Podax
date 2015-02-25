package com.axelby.podax.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class PodaxPreferenceActivity extends ActionBarActivity {

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_general);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
		    actionBar.setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment, new PodaxPreferenceFragment());
        ft.commit();
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
}
