package rx.plugins;

import rx.plugins.RxJavaPlugins;

public class RxJavaTestPlugins extends RxJavaPlugins {
	RxJavaTestPlugins() {
		super();
	}

	public static void resetPlugins(){
		getInstance().reset();
	}
}

