package com.axelby.podax.podaxapp;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface PodaxAppAPI {
	@GET("/api/search")
	List<Podcast> search(@Query("q") String query);

	@GET("/api/network/{name}")
	Observable<Network> getNetworkInfo(@Path("name") String name);
}
