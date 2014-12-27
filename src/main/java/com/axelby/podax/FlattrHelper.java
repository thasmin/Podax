package com.axelby.podax;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

import org.shredzone.flattr4j.FlattrFactory;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.exception.ForbiddenException;
import org.shredzone.flattr4j.model.AutoSubmission;
import org.shredzone.flattr4j.model.Category;
import org.shredzone.flattr4j.model.Language;
import org.shredzone.flattr4j.model.User;
import org.shredzone.flattr4j.oauth.AccessToken;
import org.shredzone.flattr4j.oauth.AndroidAuthenticator;
import org.shredzone.flattr4j.oauth.Scope;

import java.util.Arrays;
import java.util.EnumSet;

public class FlattrHelper {
	private static final String PREFS_NAME = "PodaxFlattrPref";
	private static final String PREF_OAUTH_TOKEN = "oauth_token";
	private static final String HOST_NAME = "com.axelby.podax";

	public static class NoAppSecretFlattrException extends FlattrException {
		private static final long serialVersionUID = -3922322189544852987L;
	}


	private static AccessToken getOAuthToken(Context context) {
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		String token = settings.getString(PREF_OAUTH_TOKEN, null);
		if (token == null)
			return null;
		return new AccessToken(token);
	}

	private static AndroidAuthenticator getAndroidAuthenticator(Context context) throws NoAppSecretFlattrException {
		String app_key = context.getString(R.string.flattr_api_key);
		String app_secret = context.getString(R.string.flattr_api_secret);
		if (app_key.equals("") || app_secret.equals(""))
			throw new NoAppSecretFlattrException();
		return new AndroidAuthenticator(HOST_NAME, app_key, app_secret);
	}

	public static void obtainToken(Context context) throws NoAppSecretFlattrException {
		AndroidAuthenticator auth = getAndroidAuthenticator(context);
		auth.setScope(EnumSet.of(Scope.FLATTR));

		try {
			Intent intent = auth.createAuthenticateIntent();
			context.startActivity(intent);
		} catch (FlattrException ignored) { }
	}

	public static void handleResumeActivityObtainToken(Activity context) {
		if (context == null || context.getIntent() == null)
			return;

		Uri uri = context.getIntent().getData();
		if (uri != null) {
			try {
				AndroidAuthenticator auth = getAndroidAuthenticator(context);
				AccessToken token = auth.fetchAccessToken(uri);
				if (token != null) {
					// store the AccessToken
					SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
					Editor editor = settings.edit();
					editor.putString(PREF_OAUTH_TOKEN, token.getToken());
					editor.apply();
				}
			} catch (FlattrException ignored) { }
		}
	}

	public static AutoSubmission parseAutoSubmissionLink(Uri url) {
		AutoSubmission sub = new AutoSubmission();
		// make sure it's a valid flattr url
		if (!url.getScheme().equals("https"))
			return null;
		if (!url.getHost().equals("flattr.com"))
			return null;
		if (!url.getPath().equals("/submit/auto"))
			return null;

		// some parameters are required
		if (url.getQueryParameter("user_id") == null)
			return null;
		sub.setUser(User.withId(url.getQueryParameter("user_id")));

		if (url.getQueryParameter("url") == null)
			return null;
		sub.setUrl(url.getQueryParameter("url"));

		if (url.getQueryParameter("title") == null)
			return null;
		sub.setTitle(url.getQueryParameter("title"));

		// the rest is optional
		if (url.getQueryParameter("description") != null)
			sub.setDescription(url.getQueryParameter("description"));
		if (url.getQueryParameter("language") != null)
			sub.setLanguage(Language.withId(url.getQueryParameter("language")));
		if (url.getQueryParameter("tags") != null)
			sub.setTags(Arrays.asList(url.getQueryParameter("tags").split(",")));
		if (url.getQueryParameter("hidden") != null)
			sub.setHidden(url.getQueryParameter("hidden").equals("1"));
		if (url.getQueryParameter("category") != null)
			sub.setCategory(Category.withId(url.getQueryParameter("category")));
		return sub;
	}

	public static void flattr(Context context, AutoSubmission sub) throws FlattrException {
		AccessToken token = getOAuthToken(context);
		if (token == null)
			throw new ForbiddenException("404", "No authentication token provided");
		FlattrService flattrService = FlattrFactory.getInstance().createFlattrService(token);
		flattrService.flattr(sub);
	}
}
