package com.axelby.podax;

import java.util.Vector;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class SearchActivity extends PlayerActivity {
	ExpandableListView epView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    setContentView(R.layout.search);
	    super.onCreate(savedInstanceState);

	    epView = (ExpandableListView) findViewById(R.id.results);
	    
	    // Get the intent, verify the action and get the query
	    Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	    		  SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
	      suggestions.saveRecentQuery(query, null);
		  epView.setAdapter(new SearchResultsAdapter(this, query));
	    }
	    else
		    epView.setAdapter(new SearchResultsAdapter(this));
	    epView.expandGroup(0);
	    epView.expandGroup(1);
	}	

    public class SearchResultsAdapter extends BaseExpandableListAdapter {
    	Context _context;
    	LayoutInflater _layoutInflater;
    	
    	String[] _groups = getResources().getStringArray(R.array.search_groups);
    	Vector<Subscription> _subscriptions = new Vector<Subscription>();
    	Vector<Podcast> _podcasts = new Vector<Podcast>();
    	String _query = "";

    	public SearchResultsAdapter(Context context)
    	{
    		_context = context;
    		_layoutInflater = LayoutInflater.from(_context);
    	}

    	public SearchResultsAdapter(Context context, String query)
    	{
    		_context = context;
    		_layoutInflater = LayoutInflater.from(_context);
    		_query = query;
    		_subscriptions = DBAdapter.getInstance(context).searchSubscriptions(query);
    		_podcasts = DBAdapter.getInstance(context).searchPodcasts(query);
    	}

		public Object getChild(int groupPosition, int childPosition) {
			switch (groupPosition)
			{
			case 0: return _subscriptions.get(childPosition);
			case 1: return _podcasts.get(childPosition);
			}
			return null;
		}

		public long getChildId(int groupPosition, int childPosition) {
			switch (groupPosition)
			{
			case 0: return _subscriptions.get(childPosition).getId();
			case 1: return _podcasts.get(childPosition).getId();
			}
			return -1;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView view;
			if (convertView == null) {
				view = (TextView)_layoutInflater.inflate(R.layout.list_item, null);
			}
			else {
				view = (TextView)convertView;
			}

			switch(groupPosition)
			{
			case 0:
				view.setText(_subscriptions.get(childPosition).getDisplayTitle());
				break;
			case 1:
				view.setText(_podcasts.get(childPosition).getTitle());
				break;
			}
			return view;
		}

		public int getChildrenCount(int groupPosition) {
			switch (groupPosition)
			{
			case 0: return _subscriptions.size();
			case 1: return _podcasts.size();
			}
			return -1;
		}

		public Object getGroup(int groupPosition) {
			return _groups[groupPosition];
		}

		public int getGroupCount() {
			return _groups.length;
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView view;
			if (convertView == null) {
				view = (TextView)_layoutInflater.inflate(R.layout.list_item, null);
			}
			else {
				view = (TextView)convertView;
			}
			view.setPadding(view.getPaddingLeft() + 25, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
			view.setText(_groups[groupPosition]);			
			return view;
		}

		public boolean hasStableIds() {
			return false;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

	}
}