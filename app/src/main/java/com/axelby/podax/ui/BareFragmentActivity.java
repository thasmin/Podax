package com.axelby.podax.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.axelby.podax.Constants;
import com.axelby.podax.R;

public class BareFragmentActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FrameLayout view = new FrameLayout(this);
		view.setId(R.id.fragment);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		setContentView(view, layoutParams);

		Bundle extras = getIntent().getExtras();
		String className = extras.getString(Constants.EXTRA_FRAGMENT_CLASSNAME);
		if (className == null) {
			finish();
			return;
		}

		Fragment fragment = Fragment.instantiate(this, className);
		extras.remove(Constants.EXTRA_FRAGMENT_CLASSNAME);
		fragment.setArguments(extras);
		getFragmentManager().beginTransaction().add(R.id.fragment, fragment).commit();

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null)
				actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}
}
