package com.axelby.podax;

import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.model.PodaxDB;

import org.junit.rules.ExternalResource;

public class DataCacheClearer extends ExternalResource {
	@Override
	protected void before() throws Throwable {
		super.before();

		EpisodeDB.evictCache();
		PodaxDB.subscriptions.evictCache();
	}
}
