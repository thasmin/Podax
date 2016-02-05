package com.axelby.podax;

import org.junit.rules.ExternalResource;

import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.plugins.RxJavaSchedulersHook;
import rx.plugins.RxJavaTestPlugins;
import rx.schedulers.Schedulers;

public class RxSchedulerSwitcher extends ExternalResource {
	@Override
	protected void before() throws Throwable {
		super.before();

		RxAndroidPlugins.getInstance().reset();
		RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
			@Override
			public Scheduler getMainThreadScheduler() {
				return Schedulers.immediate();
			}
		});

		RxJavaTestPlugins.resetPlugins();
		RxJavaTestPlugins.getInstance().registerSchedulersHook(new RxJavaSchedulersHook() {
			@Override
			public Scheduler getIOScheduler() {
				return Schedulers.immediate();
			}
		});
	}

	@Override
	protected void after() {
		super.after();

		RxAndroidPlugins.getInstance().reset();
	}
}
