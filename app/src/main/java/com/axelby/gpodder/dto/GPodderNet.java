package com.axelby.gpodder.dto;

import java.util.List;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface GPodderNet {
	@GET("/clientconfig.json")
	ClientConfig getConfig();

	@GET("/toplist/{count}.json")
	List<Podcast> getPodcastTopList(@Path("count") int count);

	@GET("/search.json")
	List<Podcast> search(@Query("q") String query);

	@POST("/api/2/auth/{username}/login.json")
	Response login(@Path("username") String username, @Body String emptyBodyHack);

	@POST("/api/2/auth/{username}/logout.json")
	Response logout(@Path("username") String username, @Body String emptyBodyHack);

	@GET("/api/2/devices/{username}.json")
	List<DeviceConfiguration> getDeviceList(@Path("username") String username);

	@POST("/api/2/devices/{username}/{deviceId}.json")
	Response setDeviceConfiguration(@Path("username") String username,
									@Path("deviceId") String deviceId,
									@Body DeviceConfigurationChange configuration);

	@GET("/api/2/subscriptions/{username}/{deviceId}.json")
	Changes getSubscriptionUpdates(@Path("username") String username,
								   @Path("deviceId") String deviceId,
								   @Query("since") long since);

	@POST("/api/2/subscriptions/{username}/{deviceId}.json")
	SubscriptionUpdateConfirmation uploadSubscriptionChanges(@Path("username") String username,
															 @Path("deviceId") String deviceId,
															 @Body SubscriptionChanges changes);

	@POST("/api/2/episodes/{username}.json")
	EpisodeUpdateConfirmation uploadEpisodeChanges(@Path("username") String username,
												   @Body List<EpisodeUpdate> updates);

	@GET("/api/2/episodes/{username}.json?aggregated=true")
	EpisodeUpdateResponse getEpisodeUpdates(@Path("username") String username,
											@Query("since") long since);

	@GET("/subscriptions/{username}.json")
	List<Podcast> getAllSubscriptions(@Path("username") String username);
}
