package com.axelby.podax.podaxapp;

import android.content.Context;

import com.axelby.podax.R;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class PodaxAppClient {
	private PodaxAppClient() { }

	public static PodaxAppAPI get(final Context context) {
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestInterceptor.RequestFacade request) {
						request.addHeader("User-Agent", "podax/" + context.getString(R.string.app_version));
					}
				})
				.setEndpoint("https://www.podaxapp.com")
				.setLogLevel(RestAdapter.LogLevel.FULL)
				.build();
		return restAdapter.create(PodaxAppAPI.class);
	}
}
