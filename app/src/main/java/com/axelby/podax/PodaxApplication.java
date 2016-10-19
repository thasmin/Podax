package com.axelby.podax;

import android.app.Application;
import android.os.Build;
import android.webkit.WebView;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;

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

	public static final String GPODDER_AUTHORITY = "com.axelby.podax.gpodder_sync";

	private static PodaxApplication _instance;

	public static PodaxApplication get() {
		return _instance;
	}

	@Override
	public void onCreate() {
		_instance = this;
		AppFlow.setApplication(this);
		PodaxDB.setContext(this);

        if (!PodaxLog.isDebuggable(this))
			ACRA.init(this);

		super.onCreate();

		if (PodaxLog.isDebuggable(this)) {
			WebView.setWebContentsDebuggingEnabled(true);
		}

		if (!isRoboUnitTest()) {
			MediaButtonIntentReceiver.initialize(this);
			PlayerStatus.update(this);
		}

		JodaTimeAndroid.init(this);
		PodaxLog.ensureRemoved(this);
		BootReceiver.setupAlarms(getApplicationContext());
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);

		// got on the LRU list
		if (level == TRIM_MEMORY_BACKGROUND) {
			EpisodeData.evictCache();
			SubscriptionData.evictCache();
		}
	}

	public static boolean isRoboUnitTest() {
		return "robolectric".equals(Build.FINGERPRINT);
	}
}
