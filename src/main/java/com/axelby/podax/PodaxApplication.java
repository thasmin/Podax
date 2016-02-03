package com.axelby.podax;

import android.app.Application;
import android.os.Build;
import android.webkit.WebView;

import com.facebook.stetho.Stetho;

import net.danlew.android.joda.JodaTimeAndroid;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@SuppressWarnings("WeakerAccess")
@ReportsCrashes(formKey = "",
		formUri = "http://podax.axelby.com/error",
		mode = ReportingInteractionMode.DIALOG,
		resToastText = R.string.crash_toast_text,
		resDialogText = R.string.crash_dialog_text,
		resDialogCommentPrompt = R.string.crash_comment_prompt,
		resDialogOkToast = R.string.crash_dialog_ok_text)
public class PodaxApplication extends Application {

	@Override
	public void onCreate() {
		AppFlow.setApplication(this);

        if (!PodaxLog.isDebuggable(this))
			ACRA.init(this);

		super.onCreate();

		if (PodaxLog.isDebuggable(this)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				WebView.setWebContentsDebuggingEnabled(true);
			}

			Stetho.initializeWithDefaults(this);

			/*
			Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
				PodaxLog.log(this, "error: " + ex.getMessage());
				for (StackTraceElement ste : ex.getStackTrace())
					PodaxLog.log(this, "  " + ste.toString());
				Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
				System.exit(1);
			});
			*/
		}

		JodaTimeAndroid.init(this);

		PodaxLog.ensureRemoved(this);

		if (!isRoboUnitTest())
			MediaButtonIntentReceiver.initialize(this);
		BootReceiver.setupAlarms(getApplicationContext());

		PlayerStatus.notify(this);
	}

	public static boolean isRoboUnitTest() {
		return "robolectric".equals(Build.FINGERPRINT);
	}
}
