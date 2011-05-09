package com.axelby.podax;

import android.app.Application;
import android.content.Intent;

public class PodaxApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		startService(new Intent(this, PlayerService.class));
		startService(new Intent(this, UpdateService.class));
	}

}
