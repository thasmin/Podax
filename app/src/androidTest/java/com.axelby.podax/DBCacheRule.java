package com.axelby.podax;

import com.axelby.podax.model.PodaxDB;

import org.junit.rules.ExternalResource;

public class DBCacheRule extends ExternalResource {
	@Override
	protected void before() throws Throwable {
		super.before();

		PodaxDB.episodes.evictCache();
		PodaxDB.subscriptions.evictCache();
	}
}
