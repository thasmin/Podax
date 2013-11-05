package com.axelby.podax;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;


public class Helper {

	private static RequestQueue _requestQueue = null;
	private static ImageLoader _imageLoader = null;

	public static boolean ensureWifi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnectedOrConnecting())
			return false;
		// always OK if we're on wifi
		if (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
			return true;
		// check for wifi only pref
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wifiPref", true))
			return false;
		// check for 3g data turned off
		if (!netInfo.isConnected())
			return false;

		return true;
	}

	public static String getTimeString(int milliseconds) {
		int seconds = milliseconds / 1000;
		final int SECONDSPERHOUR = 60 * 60;
		final int SECONDSPERMINUTE = 60;
		int hours = seconds / SECONDSPERHOUR;
		int minutes = seconds % SECONDSPERHOUR / SECONDSPERMINUTE;
		seconds = seconds % SECONDSPERMINUTE;

		StringBuilder builder = new StringBuilder();
		if (hours > 0) {
			builder.append(hours);
			builder.append(":");
			if (minutes < 10)
				builder.append("0");
		}
		builder.append(minutes);
		builder.append(":");
		if (seconds < 10)
			builder.append("0");
		builder.append(seconds);
		return builder.toString();
	}

	public static void registerMediaButtons(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.registerMediaButtonEventReceiver(new ComponentName(context, MediaButtonIntentReceiver.class));
	}

	public static void unregisterMediaButtons(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.unregisterMediaButtonEventReceiver(new ComponentName(context, MediaButtonIntentReceiver.class));
	}

	public static RequestQueue getRequestQueue(Context context) {
		if (_requestQueue == null)
			_requestQueue = Volley.newRequestQueue(context);
		return _requestQueue;
	}

	private static final LruCache<String, Bitmap> _imageCache = new LruCache<String, Bitmap>(10);
	public static ImageLoader getImageLoader(Context context) {
		if (_imageLoader == null) {
			_imageLoader = new ImageLoader(getRequestQueue(context), new ImageLoader.ImageCache() {

				@Override
				public Bitmap getBitmap(String url) {
					return _imageCache.get(url);
				}

				@Override
				public void putBitmap(String url, Bitmap bitmap) {
					_imageCache.put(url, bitmap);
				}
			});
		}
		return _imageLoader;
	}

	public static Bitmap getCachedImage(String url) {
		if (_imageCache.get(url) != null)
			return _imageCache.get(url);
		return _imageCache.get("#W0#H0" + url);
	}

	public static Bitmap getCachedImage(String url, int width, int height) {
		String lookup = "#W" + width + "#H" + height + url;
		if (_imageCache.get(lookup) != null)
			return _imageCache.get(lookup);
		Bitmap fullSize = getCachedImage(url);
		if (fullSize == null)
			return null;
		Bitmap scaled = Bitmap.createScaledBitmap(fullSize, width, height, false);
		_imageCache.put(lookup, scaled);
		return scaled;
	}
}
