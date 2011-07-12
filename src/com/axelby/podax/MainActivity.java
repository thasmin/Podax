package com.axelby.podax;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		RelativeLayout _queue = (RelativeLayout) this.findViewById(R.id.queue);
		_queue.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, QueueActivity.class);
				startActivity(intent);
			}
		});


		RelativeLayout subscriptions = (RelativeLayout) this.findViewById(R.id.subscriptions);
		subscriptions.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, SubscriptionListActivity.class);
				startActivity(intent);
			}
		});

		RelativeLayout readerimport = (RelativeLayout) this.findViewById(R.id.readerimport);
		readerimport.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, GoogleAccountChooserActivity.class);
				startActivity(intent);
			}
		});

		RelativeLayout activedownloads = (RelativeLayout) this.findViewById(R.id.activedownloads);
		activedownloads.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, ActiveDownloadListActivity.class);
				startActivity(intent);
			}
		});
		
		PlayerActivity.injectPlayerFooter(this);
	}
}