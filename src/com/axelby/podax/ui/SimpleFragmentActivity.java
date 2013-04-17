package com.axelby.podax.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class SimpleFragmentActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String fragmentClass = getIntent().getStringExtra("com.axelby.podax.fragment_class");
		if (fragmentClass != null)
			createFragment(fragmentClass);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Helper.registerMediaButtons(this);
	}

	protected void createFragment(Class<?> fragmentClass) {
		this.createFragment(fragmentClass.getCanonicalName());
	}

	public void createFragment(String fragmentClass) {
		FrameLayout frame = new FrameLayout(this);
		frame.setId(R.id.fragment);
		setContentView(frame, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		Fragment fragment = Fragment.instantiate(this, fragmentClass);
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.fragment, fragment);
		ft.commit();
	}
}
