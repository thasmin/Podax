package com.axelby.podax;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends PlayerActivity {
	public class DashboardAdapter extends BaseAdapter {
		private final String[] ITEMS = new String[] { "Queue", "Subscriptions", "Import from Google Reader" };
		private Context _context;
		
		public DashboardAdapter(Context context) {
			super();
			_context = context;
		}

		public int getCount() {
			return ITEMS.length;
		}

		public Object getItem(int position) {
			return ITEMS[position];
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view;
			if (convertView == null) {
				view = new TextView(_context);
				view.setText(ITEMS[position]);
				view.setGravity(Gravity.CENTER_HORIZONTAL);
			} else {
				view = (TextView)convertView;
			}
			return view;
		}

	}

	GridView _grid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.main);
		
		super.onCreate(savedInstanceState);

		_grid = (GridView) this.findViewById(R.id.grid);
		_grid.setAdapter(new DashboardAdapter(this));

		_grid.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();
				switch (position) {
				case 0:
					intent.setClassName("com.axelby.podax", "com.axelby.podax.QueueActivity");
					startActivity(intent);
					break;
				case 1:
					intent.setClassName("com.axelby.podax", "com.axelby.podax.SubscriptionListActivity");
					startActivity(intent);
					break;
				case 2:
					intent.setClassName("com.axelby.podax", "com.axelby.podax.GoogleAccountChooserActivity");
					startActivity(intent);
					break;
				}
			}

		});
	}

}
