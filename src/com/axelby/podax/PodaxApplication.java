package com.axelby.podax;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dFNuZ3FSYW56MXhVVEViV2tpR2xYdEE6MQ", 
				mode = ReportingInteractionMode.TOAST,
				resToastText = R.string.crash_toast_text,
				customReportContent = {
					org.acra.ReportField.USER_COMMENT,
					org.acra.ReportField.ANDROID_VERSION,
					org.acra.ReportField.APP_VERSION_NAME,
					org.acra.ReportField.BRAND,
					org.acra.ReportField.PHONE_MODEL,
					org.acra.ReportField.CUSTOM_DATA,
					org.acra.ReportField.STACK_TRACE,
				})
public class PodaxApplication extends Application {

	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();
	}

}
