package com.axelby.podax;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
	public static final String AUTHORITY = "com.axelby.podax.searchsuggestionprovider";
	public static final int MODE = DATABASE_MODE_QUERIES;

	public SearchSuggestionProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}
}
