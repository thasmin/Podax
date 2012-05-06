package com.axelby.podax.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.axelby.podax.Constants;
import com.axelby.podax.R;

public class DiscoverActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discover);

		TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent;

	    intent = new Intent().setClass(this, PopularSubscriptionListActivity.class);
	    intent.putExtra(Constants.EXTRA_POPULAR_SOURCE_NAME, "Podax server");
	    intent.putExtra(Constants.EXTRA_POPULAR_SOURCE_URL, "http://podax.axelby.com/popular.php");
		spec = tabHost
				.newTabSpec("podax")
				.setIndicator("Podax",
						getResources().getDrawable(R.drawable.ic_tab_podax))
				.setContent(intent);
		tabHost.addTab(spec);

	    intent = new Intent().setClass(this, PopularSubscriptionListActivity.class);
	    intent.putExtra(Constants.EXTRA_POPULAR_SOURCE_NAME, "iTunes");
	    intent.putExtra(Constants.EXTRA_POPULAR_SOURCE_URL, "http://podax.axelby.com/popularitunes.php");
		spec = tabHost
				.newTabSpec("itunes")
				.setIndicator("iTunes",
						getResources().getDrawable(R.drawable.ic_tab_apple))
				.setContent(intent);
	    tabHost.addTab(spec);
	}

}
