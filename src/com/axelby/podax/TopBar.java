package com.axelby.podax;

import com.axelby.podax.ui.MainActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TopBar extends LinearLayout {

	ImageButton homebtn;
	TextView title;
	
	public TopBar(Context context) {
		super(context);
		
		loadViews(context);
	}

	public TopBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater.from(context).inflate(R.layout.topbar, this);
		
		loadViews(context);
	}

	private void loadViews(final Context context) {
		homebtn = (ImageButton) findViewById(R.id.ab_home);
		homebtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(context, MainActivity.class);
				context.startActivity(intent);
			}
		});

		title = (TextView) findViewById(R.id.ab_title);
		if (context instanceof Activity)
			title.setText(((Activity)context).getTitle());
	}
}
