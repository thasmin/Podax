package com.axelby.podax.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.axelby.podax.BootReceiver;
import com.axelby.podax.R;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		View _queue = this.findViewById(R.id.queue);
		_queue.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, QueueActivity.class);
				startActivity(intent);
			}
		});


		View subscriptions = this.findViewById(R.id.subscriptions);
		subscriptions.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, SubscriptionListActivity.class);
				startActivity(intent);
			}
		});

		View activedownloads = this.findViewById(R.id.activedownloads);
		activedownloads.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, ActiveDownloadListActivity.class);
				startActivity(intent);
			}
		});

		View about = this.findViewById(R.id.about);
		about.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, AboutActivity.class);
				startActivity(intent);
			}
		});
	}

    @Override
	protected void onResume() {
		super.onResume();

		BootReceiver.setupAlarms(this);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		//menu.add(0, 0, 0, "Start Profiler");
		//menu.add(0, 1, 0, "Stop Profiler");
		menu.add(0, 2, 0, "Preferences");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Debug.startMethodTracing("podax");
			break;
		case 1:
			Debug.stopMethodTracing();
			break;
		case 2:
			startActivity(new Intent(this, Preferences.class));
			break;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
    }

}