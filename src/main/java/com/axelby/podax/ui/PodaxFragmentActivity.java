package com.axelby.podax.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class PodaxFragmentActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (getActionBar() != null)
		    getActionBar().setDisplayHomeAsUpEnabled(true);
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

	protected Fragment createFragment(Class<?> fragmentClass) {
        return createFragment(fragmentClass, null);
	}

    protected Fragment createFragment(Class<?> fragmentClass, Bundle arguments) {
        FrameLayout frame = new FrameLayout(this);
        frame.setId(R.id.fragment);
        setContentView(frame, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        Fragment fragment = Fragment.instantiate(this, fragmentClass.getCanonicalName());
        fragment.setArguments(arguments);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment, fragment);
        ft.commit();

        return fragment;
    }
}
