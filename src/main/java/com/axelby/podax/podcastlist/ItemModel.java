package com.axelby.podax.podcastlist;

import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

import com.axelby.podax.AppFlow;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.squareup.picasso.Picasso;

public class ItemModel {
	public enum RSSSource {ITunes, RSS, SubscriptionId}

	private final String _imageUrl;
	private final String _title;
	private final RSSSource _source;
	private final String _rssUrl;
	private final long _subscriptionId;

	public static ItemModel fromRSS(String title, String imageUrl, String rssUrl) {
		return new ItemModel(title, imageUrl, RSSSource.RSS, rssUrl);
	}

	public static ItemModel fromITunes(String title, String imageUrl, String itunesUrl) {
		return new ItemModel(title, imageUrl, RSSSource.ITunes, itunesUrl);
	}

	public static ItemModel fromSubscriptionId(String title, String imageUrl, long subscriptionId) {
		return new ItemModel(title, imageUrl, subscriptionId);
	}

	private ItemModel(String title, String imageUrl, RSSSource source, String rssUrl) {
		_title = title;
		_imageUrl = imageUrl;
		_source = source;
		_rssUrl = rssUrl;
		_subscriptionId = -1;
	}

	private ItemModel(String title, String imageUrl, long subscriptionId) {
		_title = title;
		_imageUrl = imageUrl;
		_source = RSSSource.SubscriptionId;
		_rssUrl = null;
		_subscriptionId = subscriptionId;
	}

	public String getImageUrl() {
		return _imageUrl;
	}

	public String getTitle() {
		return _title;
	}

	public void show(View view) {
		View thumbnail = view.findViewById(R.id.thumbnail);
		View title = view.findViewById(R.id.title);
		switch (_source) {
			case RSS:
				AppFlow.get(Helper.getActivityFromView(view)).displayPodcastViaRSSUrl(_title, _rssUrl, thumbnail, title);
				break;
			case ITunes:
				AppFlow.get(Helper.getActivityFromView(view)).displayPodcastViaITunes(_title, _rssUrl, thumbnail, title);
				break;
			case SubscriptionId:
				AppFlow.get(Helper.getActivityFromView(view)).displaySubscription(_title, _subscriptionId, thumbnail, title);
				break;
		}
	}

	@BindingAdapter("app:picassoImageUrl")
	@SuppressWarnings("unused")
	public static void loadImageUrlViaPicasso(ImageView image, String url) {
		if (url == null || url.length() == 0)
			return;
		Picasso.with(image.getContext()).load(url).fit().into(image);
	}

}
