package com.axelby.podax.podaxapp;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Query;

public interface PodaxAppAPI {
	@GET("/api/search")
	List<Podcast> search(@Query("q") String query);
}
