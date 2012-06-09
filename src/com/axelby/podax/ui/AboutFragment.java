package com.axelby.podax.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;
import com.axelby.podax.R;

public class AboutFragment extends SherlockFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.about, null, false);
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		WebView webview = (WebView) getActivity().findViewById(R.id.webview);
		String html = "<html><head><style type=\"text/css\">" +
				"a { color: #E59F39 }" +
				"</style></head>" +
				"<body style=\"background:black;color:white\">" + getString(R.string.about_content) + "</body></html>"; 
		webview.loadData(html, "text/html", "utf-8");
		webview.setBackgroundColor(Color.BLACK);
	}

}
