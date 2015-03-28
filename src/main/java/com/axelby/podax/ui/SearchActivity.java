package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;

import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class SearchActivity extends ActionBarActivity {
	private String _query = "";

	public interface QueryChangedHandler {
		public void onQueryChanged(String query);
	}
	private QueryChangedHandler[] _fragments = new QueryChangedHandler[3];

	public class SearchAdapter extends FragmentPagerAdapter {
		public SearchAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public Fragment getItem(int position) {
			if (_fragments[position] == null) {
				switch (position) {
					case 0: _fragments[position] = new SearchPodaxFragment(); break;
					case 1: _fragments[position] = new SearchPodaxAppFragment(); break;
					case 2: return new Fragment();
					default: return null;
				}
				Fragment fragment = (Fragment)_fragments[position];
				Bundle bundle = new Bundle(1);
				bundle.putString(SearchManager.QUERY, _query);
				fragment.setArguments(bundle);
			}

			return (Fragment) _fragments[position];
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0: return "Subscriptions";
				case 1: return "podaxapp.com";
				case 2: return "gpodder.net";
				default: return "";
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_activity);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
		    actionBar.setDisplayHomeAsUpEnabled(true);

		// Get the intent, verify the action and get the query
		Intent intent = getIntent();
		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
			finish();
			return;
		}

		EditText query = (EditText) findViewById(R.id.query);
		_query = intent.getStringExtra(SearchManager.QUERY);
		query.setText(_query);
		query.setSelection(query.getText().length());
		query.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence str, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}

			@Override
			public void onTextChanged(CharSequence str, int start, int before, int count) {
				_query = str.toString();
				for (QueryChangedHandler fragment : _fragments)
					if (fragment != null)
						fragment.onQueryChanged(_query);
			}
		});

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(new SearchAdapter(getFragmentManager()));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Helper.registerMediaButtons(this);
	}
}
