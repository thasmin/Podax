package com.axelby.podax;

import org.junit.rules.ExternalResource;

public class DataCacheClearer extends ExternalResource {
	@Override
	protected void before() throws Throwable {
		super.before();

		Episodes.evictCache();
		Subscriptions.evictCache();
	}
}
