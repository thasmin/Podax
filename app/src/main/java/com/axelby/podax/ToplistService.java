package com.axelby.podax;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.axelby.podax.itunes.PodcastFetcher;

import java.util.TreeMap;

public class ToplistService extends Service {
	public ToplistService() { }

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		TreeMap<Integer, String> categoryNames = new TreeMap<>();
		categoryNames.put(0, "All");
		categoryNames.put(1301, "Arts");
		categoryNames.put(1321, "Business");
		categoryNames.put(1303, "Comedy");
		categoryNames.put(1304, "Education");
		categoryNames.put(1323, "Games & Hobbies");
		categoryNames.put(1325, "Government & Organizations");
		categoryNames.put(1307, "Health");
		categoryNames.put(1305, "Kids");
		categoryNames.put(1310, "Music");
		categoryNames.put(1311, "News & Politics");
		categoryNames.put(1314, "Religion & Spirituality");
		categoryNames.put(1315, "Science & Medicine");
		categoryNames.put(1324, "Society & Culture");
		categoryNames.put(1316, "Sports & Recreation");
		categoryNames.put(1309, "TV & Film");
		categoryNames.put(1318, "Technology");

		int[] categories = new int[]{
			0, 1301, 1321, 1303, 1304, 1323, 1325, 1307, 1305,
			1310, 1311, 1314, 1315, 1324, 1316, 1309, 1318,
		};

		for (int cat : categories) {
			try {
				new PodcastFetcher(this, cat).getPodcasts().toBlocking().first();
			} catch (Exception e) {
				Log.e("ToplistService", String.format("unable to retrieve category %1$d: %2$s", cat, categoryNames.get(cat)), e);
			}
		}

		return START_NOT_STICKY;
	}
}
