package com.axelby.podax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

public class MainActivity extends PlayerActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		RelativeLayout _queue = (RelativeLayout) this.findViewById(R.id.queue);
		_queue.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClassName("com.axelby.podax", "com.axelby.podax.QueueActivity");
				startActivity(intent);
			}
		});


		RelativeLayout subscriptions = (RelativeLayout) this.findViewById(R.id.subscriptions);
		subscriptions.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClassName("com.axelby.podax", "com.axelby.podax.SubscriptionListActivity");
				startActivity(intent);
			}
		});

		RelativeLayout readerimport = (RelativeLayout) this.findViewById(R.id.readerimport);
		readerimport.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClassName("com.axelby.podax", "com.axelby.podax.GoogleAccountChooserActivity");
				startActivity(intent);
			}
		});
	}
}