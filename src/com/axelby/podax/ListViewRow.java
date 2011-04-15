package com.axelby.podax;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

class ListViewRow extends LinearLayout {
	private TextView _textview;

	public ListViewRow(Context context) {
		super(context);
		this.setOrientation(VERTICAL);
		this.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		_textview = new TextView(context);
		_textview.setText("URL");
		// copy simple_list_item_1 style
		_textview.setTextSize(19);
		_textview.setPadding(3, 3, 3, 3);
		_textview.setGravity(Gravity.CENTER_VERTICAL);
		_textview.setMinHeight(55);
		addView(_textview,
				new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT, 
						LayoutParams.FILL_PARENT));
	}
	
	public void setText(CharSequence text) {
		_textview.setText(text);
	}
	
}
