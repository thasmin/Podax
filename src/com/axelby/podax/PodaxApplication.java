package com.axelby.podax;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dFNuZ3FSYW56MXhVVEViV2tpR2xYdEE6MQ", 
				mode = ReportingInteractionMode.TOAST,
				resToastText = R.string.crash_toast_text)
public class PodaxApplication extends Application {

	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();

		PodaxLog.ensureRemoved(this);
	}

}
