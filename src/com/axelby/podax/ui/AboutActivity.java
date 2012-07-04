package com.axelby.podax.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class AboutActivity extends SherlockActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.about);
		WebView webview = (WebView) findViewById(R.id.webview);
		String html = "<html><head><style type=\"text/css\">" +
				"a { color: #E59F39 }" +
				"</style></head>" +
				"<body style=\"background:black;color:white\">" + getString(R.string.about_content) + "</body></html>"; 
		webview.loadData(html, "text/html", "utf-8");
		webview.setBackgroundColor(Color.BLACK);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Helper.registerMediaButtons(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, MainActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

}
