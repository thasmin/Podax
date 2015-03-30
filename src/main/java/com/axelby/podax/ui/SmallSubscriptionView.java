package com.axelby.podax.ui;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.squareup.picasso.Picasso;

public class SmallSubscriptionView extends FrameLayout {
	private ImageView _thumbnail;
	private TextView _title;
	private String _rssUrl;

	private OnClickListener _clickHandler = new OnClickListener() {
		@Override
		public void onClick(View view) {
			Uri uri = SubscriptionProvider.addSingleUseSubscription(getContext(), _rssUrl);
			long subscriptionId = ContentUris.parseId(uri);
			Intent intent = PodaxFragmentActivity.createIntent(getContext(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
			getContext().startActivity(intent);
		}
	};

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
		_thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
		_title = (TextView) view.findViewById(R.id.title);

		view.setOnClickListener(_clickHandler);
	}

	public void set(String title, String imageUrl, String rssUrl) {
		_title.setText(title);
		Picasso.with(getContext()).load(imageUrl).into(_thumbnail);
		_rssUrl = rssUrl;
	}

	public ViewHolder getViewHolder() {
		return new ViewHolder(this);
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public ViewHolder(SmallSubscriptionView view) {
			super(view);
		}

		public void set(String title, String imageUrl, String rssUrl) {
			SmallSubscriptionView.this.set(title, imageUrl, rssUrl);
		}
	}
}
