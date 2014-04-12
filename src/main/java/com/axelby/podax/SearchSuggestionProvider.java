package com.axelby.podax;

import android.content.SearchRecentSuggestionsProvider;
import android.net.Uri;

public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
	public static final String AUTHORITY = "com.axelby.podax.searchsuggestionprovider";
	public static final int MODE = DATABASE_MODE_QUERIES;

	// uri taken from SearchRecentSuggestions class from Android src
	public static final Uri URI = Uri.parse("content://" + AUTHORITY);

	public SearchSuggestionProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}
}
