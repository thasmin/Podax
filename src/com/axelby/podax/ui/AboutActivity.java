package com.axelby.podax.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

import com.axelby.podax.R;

public class AboutActivity extends PodaxActivity {

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
	}

}
