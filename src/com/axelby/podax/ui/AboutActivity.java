package com.axelby.podax.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.Button;

import com.axelby.podax.PodaxLog;
import com.axelby.podax.R;

public class AboutActivity extends PodaxActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.about);
		WebView webview = (WebView) findViewById(R.id.webview);
		String content = getString(R.string.about_content);
		String html = "<html><head><style type=\"text/css\">" +
				"a { color: #E59F39 }" +
				"</style></head>" +
				"<body style=\"background:transparent;color:white\">" + content + "</body></html>";
		webview.loadData(html, "text/html", "utf-8");
		webview.setBackgroundColor(0x00000000);

		if (PodaxLog.isDebuggable(this)) {
			ViewGroup parent = (ViewGroup) webview.getParent();
			Button button = new Button(this);
			button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			button.setText("Log Viewer");
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					startActivity(new Intent(AboutActivity.this, LogViewer.class));
				}
			});
			parent.addView(button, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
	}

}
