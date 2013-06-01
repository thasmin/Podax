package com.axelby.podax;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

public class StartActivityOnClickListener implements OnClickListener {
	Context _context;
	Class<? extends Activity> _activityClass;

	public StartActivityOnClickListener(Context context, Class<? extends Activity> activityClass) {
		_context = context;
		_activityClass = activityClass;
	}

	@Override
	public void onClick(View view) {
		_context.startActivity(new Intent(_context, _activityClass));
	}
}