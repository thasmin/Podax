package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class ActivePodcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// only receives com.axelby.podax.activepodcast intents
		Uri activePodcastUri = PodcastProvider.ACTIVE_PODCAST_URI;
		if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_RESTART))
			PodcastProvider.restart(context, activePodcastUri);
		else if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_BACK))
			PodcastProvider.movePositionBy(context, activePodcastUri, -15);
		else if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_FORWARD))
			PodcastProvider.movePositionBy(context, activePodcastUri, 30);
		else if (intent.getData().equals(Constants.ACTIVE_PODCAST_DATA_END))
			PodcastProvider.skipToEnd(context, activePodcastUri);
	}

}
