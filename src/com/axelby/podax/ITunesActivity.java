package com.axelby.podax;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ITunesActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.innerlist);

		setListAdapter(new ITunesCategoryAdapter());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (id == 0L) {
			super.onListItemClick(l, v, position, id);
			return;
		}

		/*
		Intent intent = new Intent(this, ITunesRSSActivity.class);
		intent.putExtra(Constants.EXTRA_CATEGORY, id);
		startActivity(intent);
		*/
	}

	public class ITunesCategoryAdapter extends BaseAdapter {
		private LayoutInflater _inflater;

		private int[] categoryCodes = {
				1301, // Arts
				1321, // Business
				1303, // Comedy
				1304, // Education
				1323, // Games & Hobbies
				1325, // Government & Organizations
				1307, // Health
				1305, // Kids & Family
				1310, // Music
				1311, // News & Politics
				1314, // Religion & Spirituality
				1315, // Science & Medicine
				1324, // Society & Culture
				1316, // Sports & Recreation
				1318, // Technology
				1309, // TV & Film
		};
		private String[] options = { "Categories",
				"Arts",
				"Business",
				"Comedy",
				"Education",
				"Games & Hobbies",
				"Government & Organizations",
				"Health",
				"Kids & Family",
				"Music",
				"News & Politics",
				"Religion & Spirituality",
				"Science & Medicine",
				"Society & Culture",
				"Sports & Recreation",
				"Technology",
				"TV & Film",
		};

		public ITunesCategoryAdapter() {
			super();

			_inflater = LayoutInflater.from(ITunesActivity.this);
		}

		public int getCount() {
			return options.length;
		}

		public Object getItem(int position) {
			return options[position];
		}

		public long getItemId(int position) {
			if (position == 0)
				return 0L;
			return categoryCodes[position - 1];
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv;
			if (position == 0) {
				tv = new TextView(ITunesActivity.this);
				tv.setTextAppearance(ITunesActivity.this, android.R.style.TextAppearance_Medium);
				tv.setBackgroundDrawable(getResources().getDrawable(R.drawable.back));
			} else {
				tv = (TextView) _inflater.inflate(android.R.layout.simple_list_item_1, null);
				tv.setTextAppearance(ITunesActivity.this, android.R.style.TextAppearance_Large);
				tv.setBackgroundDrawable(null);
			}

			tv.setText(options[position]);
			return tv;
		}

	}
}
