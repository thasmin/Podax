package com.axelby.podax;

import java.util.Vector;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PodcastListActivity extends ListActivity {
	private int _subscriptionId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.podcast_list);
        
        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        _subscriptionId = bundle.getInt("subscriptionId");
        
        getListView().setAdapter(new PodcastAdapter(this));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.podcast_list, menu);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.refresh_subscription:
        	Subscription subscription = DBAdapter.getInstance(this).loadSubscription(_subscriptionId);
            SubscriptionUpdateService.getInstance().updateSubscription(subscription);
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
    	Intent intent = new Intent();
    	intent.setClassName("com.axelby.podax", "com.axelby.podax.PodcastDetailActivity");
    	Podcast pod = (Podcast)list.getItemAtPosition(position);
    	intent.putExtra("podcastId", pod.getId());
    	startActivity(intent);
    }

    private class PodcastAdapter extends BaseAdapter {
    	private Context _context;
    	private LayoutInflater _layoutInflater;
    	private Vector<Podcast> _podcasts;
    	
    	public PodcastAdapter(Context context) {
    		_context = context;
    		_layoutInflater = LayoutInflater.from(_context);
    		_podcasts = DBAdapter.getInstance(_context).getPodcastsForSubscription(PodcastListActivity.this._subscriptionId);
    	}
    	
		public int getCount() {
			return _podcasts.size();
		}

		public Object getItem(int position) {
			return _podcasts.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view;
			if (convertView == null) {
				view = (TextView)_layoutInflater.inflate(R.layout.list_item, null);
			}
			else {
				view = (TextView)convertView;
			}
			
			view.setText(_podcasts.get(position).getTitle());
			
			return view;
		}
    }
}
