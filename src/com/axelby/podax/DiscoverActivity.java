package com.axelby.podax;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DiscoverActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discover);

		setListAdapter(new DiscoverAdapter());
	}


	public class DiscoverAdapter extends BaseAdapter {
		private LayoutInflater _inflater;
		private String[] options = { "Popular iTunes Feeds",
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

		public DiscoverAdapter() {
			super();

			_inflater = LayoutInflater.from(DiscoverActivity.this);
		}

		public int getCount() {
			return options.length;
		}

		public Object getItem(int position) {
			return options[position];
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv;
			if (position == 0) {
				tv = new TextView(DiscoverActivity.this);
				tv.setTextAppearance(DiscoverActivity.this, android.R.style.TextAppearance_Medium);
				tv.setBackgroundDrawable(getResources().getDrawable(R.drawable.back));
			} else {
				tv = (TextView) _inflater.inflate(android.R.layout.simple_list_item_1, null);
				tv.setTextAppearance(DiscoverActivity.this, android.R.style.TextAppearance_Large);
				tv.setBackgroundDrawable(null);
			}

			tv.setText(options[position]);
			return tv;
		}

	}
}
