package com.axelby.podax.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.axelby.podax.R;

public class SubscriptionSettingsActivity extends PodaxFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FrameLayout layout = new FrameLayout(this);
		layout.setId(R.id.layout);		
		addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		Fragment fragment = new SubscriptionSettingsFragment();
		fragment.setArguments(getIntent().getExtras());

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(R.id.layout, fragment);
		transaction.commit();
	}

}
