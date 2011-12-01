package com.axelby.podax;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class DiscoverActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discover);

		TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent;

	    intent = new Intent().setClass(this, PopularPodaxActivity.class);
		spec = tabHost
				.newTabSpec("podax")
				.setIndicator("Podax",
						getResources().getDrawable(R.drawable.ic_tab_podax))
				.setContent(intent);
		tabHost.addTab(spec);

	    intent = new Intent().setClass(this, ITunesActivity.class);
		spec = tabHost.newTabSpec("itunes").setIndicator("iTunes")
				.setContent(intent);
	    tabHost.addTab(spec);
	}

}
