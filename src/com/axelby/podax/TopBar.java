package com.axelby.podax;

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
	
	/*
	public static void inject(final Activity activity) {
		PlayerActivity.injectView(activity, R.layout.topbar);

		// remove title bar
		View v = activity.findViewById(android.R.id.title);
		if (v != null) {
			v.setPadding(0, 0, 0, 0);
			ViewGroup p1 = (ViewGroup)v.getParent();
			ViewGroup p2 = (ViewGroup)p1.getParent();
			p2.removeView(p1);
		}

		
		TextView title = (TextView)activity.findViewById(R.id.ab_title);
		title.setText(activity.getTitle());
		
		ImageView home = (ImageView)activity.findViewById(R.id.ab_home);
		home.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(activity, "go home", Toast.LENGTH_SHORT).show();
			}
		});
	}
	*/
}
