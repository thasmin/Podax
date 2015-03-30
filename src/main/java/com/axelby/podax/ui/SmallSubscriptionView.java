package com.axelby.podax.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.R;
import com.squareup.picasso.Picasso;

public class SmallSubscriptionView extends FrameLayout {
	private ImageView thumbnail;
	private TextView title;

	public SmallSubscriptionView(Context context) {
		super(context);
		initView();
	}

	public SmallSubscriptionView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public SmallSubscriptionView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	private void initView() {
		View view = LayoutInflater.from(getContext()).inflate(R.layout.search_item_subscription, this, false);
		addView(view);
		thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
		title = (TextView) view.findViewById(R.id.title);
	}

	public void set(String title, String imageUrl) {
		this.title.setText(title);
		Picasso.with(getContext()).load(imageUrl).into(thumbnail);
	}

	public ViewHolder getViewHolder() {
		return new ViewHolder(this);
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public ViewHolder(SmallSubscriptionView view) {
			super(view);
		}

		public void set(String title, String imageUrl) {
			SmallSubscriptionView.this.set(title, imageUrl);
		}
	}
}
