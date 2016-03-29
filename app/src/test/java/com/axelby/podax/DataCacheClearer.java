package com.axelby.podax;

import com.axelby.podax.model.Episodes;
import com.axelby.podax.model.PodaxDB;

import org.junit.rules.ExternalResource;

public class DataCacheClearer extends ExternalResource {
	@Override
	protected void before() throws Throwable {
		super.before();

		Episodes.evictCache();
		PodaxDB.subscriptions.evictCache();
	}
}
