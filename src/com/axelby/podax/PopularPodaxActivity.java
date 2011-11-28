package com.axelby.podax;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PopularPodaxActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.innerlist);
		String[] strings = { "Loading from Podax server..." };
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, strings));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}

}
