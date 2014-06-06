package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class AdManager {

	private final InterstitialAd _interstitialAd;
	private final Context _context;

	public AdManager(Context context) {
		_context = context;

		_interstitialAd = new InterstitialAd(context);
		_interstitialAd.setAdUnitId("ca-app-pub-7211612613879292/4292414964");
		_interstitialAd.loadAd(new AdRequest.Builder().addKeyword("podcast").build());
		_interstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdClosed() {
				super.onAdClosed();
				_interstitialAd.loadAd(new AdRequest.Builder().addKeyword("podcast").build());
			}

			@Override
			public void onAdFailedToLoad(int errorCode) {
				super.onAdFailedToLoad(errorCode);
				_interstitialAd.loadAd(new AdRequest.Builder().addKeyword("podcast").build());
			}

			@Override
			public void onAdLeftApplication() {
				super.onAdLeftApplication();
				_interstitialAd.loadAd(new AdRequest.Builder().addKeyword("podcast").build());
			}
		});
	}

	public void setAdTime() {
		SharedPreferences statsPrefs = _context.getSharedPreferences("ads", Context.MODE_PRIVATE);
		float listenTime = Stats.getTime(_context);
		statsPrefs.edit().putFloat("adTime", listenTime).commit();
	}

	public float getTimeSinceLastAd() {
		SharedPreferences statsPrefs = _context.getSharedPreferences("ads", Context.MODE_PRIVATE);
		float listenTime = Stats.getTime(_context);
		float adTime = statsPrefs.getFloat("adTime", 0f);
		return listenTime - adTime;
	}

	public void showAd() {
		if (!PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("showAds", true))
			return;

		// show ads once per hour of listening time
		final int SECONDS_PER_HOUR = 60 * 60;
		if (getTimeSinceLastAd() < SECONDS_PER_HOUR)
			return;

		if (_interstitialAd.isLoaded()) {
			setAdTime();
			_interstitialAd.show();
		}
	}
}
