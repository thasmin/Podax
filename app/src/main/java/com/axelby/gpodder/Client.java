package com.axelby.gpodder;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.axelby.gpodder.dto.Changes;
import com.axelby.gpodder.dto.ClientConfig;
import com.axelby.gpodder.dto.DeviceConfiguration;
import com.axelby.gpodder.dto.DeviceConfigurationChange;
import com.axelby.gpodder.dto.EpisodeUpdate;
import com.axelby.gpodder.dto.EpisodeUpdateConfirmation;
import com.axelby.gpodder.dto.EpisodeUpdateResponse;
import com.axelby.gpodder.dto.GPodderNet;
import com.axelby.gpodder.dto.Podcast;
import com.axelby.gpodder.dto.SubscriptionChanges;
import com.axelby.podax.R;
import com.axelby.podax.model.PodaxDB;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;

public class Client {
	private String _username = null;
	private String _password = null;
	private String _sessionId;

	private Context _context;
	private GPodderNet _service;

	private static Calendar _configRefresh = null;
	private RequestInterceptor _requestInterceptor = new RequestInterceptor() {
		@Override
		public void intercept(RequestFacade request) {
			request.addHeader("User-Agent", "podax/" + _context.getString(R.string.app_version));
			if (_sessionId != null) {
				request.addHeader("Cookie", "sessionid=" + _sessionId);
			} else if (_username != null && _password != null) {
				// basic authentication
				String auth = _username + ":" + _password;
				String encoded = new String(Base64.encode(auth.getBytes(), Base64.NO_WRAP));
				request.addHeader("Authorization", "basic " + encoded);
			}
		}
	};

	public Client() { }

	public Client(Context context, String username, String password) {
		this();

		_context = context;
		_username = username;
		_password = password;
	}

	private String _errorMessage = null;
	public String getErrorMessage() { return _errorMessage; }

	private boolean verifyCurrentConfig() {
		_errorMessage = null;
		if (_configRefresh == null || _configRefresh.before(new GregorianCalendar())) {
			try {
				ClientConfig config = retrieveGPodderConfig();

				// do NOT use basic auth over HTTP without SSL
				if (config.mygpo.baseurl.startsWith("http://"))
					config.mygpo.baseurl = "https://" + config.mygpo.baseurl.substring(7);

				RestAdapter restAdapter = new RestAdapter.Builder()
						.setEndpoint(config.mygpo.baseurl)
						.setRequestInterceptor(_requestInterceptor)
						.setLogLevel(RestAdapter.LogLevel.FULL)
						.build();
				_service = restAdapter.create(GPodderNet.class);

				_configRefresh = new GregorianCalendar();
				_configRefresh.add(Calendar.MILLISECOND, config.update_timeout);
				return true;
			} catch (RetrofitError e) {
				_errorMessage = _context.getString(R.string.gpodder_sync_error);
				return false;
			}
		}
		_errorMessage = _context.getString(R.string.gpodder_sync_error);
		return false;
	}

	private ClientConfig retrieveGPodderConfig() {
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setRequestInterceptor(request ->
					request.addHeader("User-Agent", "podax/" + _context.getString(R.string.app_version))
				)
				.setEndpoint("http://gpodder.net")
				.setLogLevel(RestAdapter.LogLevel.FULL)
				.build();
		GPodderNet service = restAdapter.create(GPodderNet.class);
		return service.getConfig();
	}

	public List<Podcast> getPodcastToplist(int count) {
		if (!verifyCurrentConfig())
			return null;
		try {
			return _service.getPodcastTopList(count);
		} catch (RetrofitError e) {
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
			return null;
		}
	}

	public boolean login() {
		if (!verifyCurrentConfig())
			return false;

		try {
			Response response = _service.login(_username, "hack");
			if (response == null) {
				_errorMessage = _context.getString(R.string.gpodder_sync_timeout);
				return false;
			}
			return findSessionId(response.getHeaders());
		} catch (RetrofitError e) {
			if (e.getResponse().getStatus() == 401)
				_errorMessage = _context.getString(R.string.gpodder_sync_cannot_authenticate);
			else {
				Log.e("gpodder.client", e.getMessage());
				_errorMessage = _context.getString(R.string.gpodder_sync_error);
			}
			return false;
		}
	}

	private boolean findSessionId(List<Header> headers) {
		for (Header header : headers) {
			if (header.getName() == null || !header.getName().equals("Set-Cookie"))
				continue;

			String[] data = header.getValue().split(";")[0].split("=");
			if (data[0].equals("sessionid")) {
				_sessionId = data[1];
				return true;
			}
		}
		return false;
	}

	public boolean logout() {
		if (!verifyCurrentConfig())
			return false;

		try {
			_sessionId = null;
			_service.logout(_username, "hack");
			return true;
		} catch (RetrofitError e) {
			Log.e("gpodder.client", e.getMessage());
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
			return false;
		}
	}

	public void setDeviceConfiguration(String deviceId, DeviceConfiguration configuration) {
		if (!verifyCurrentConfig())
			return;
		try {
			_service.setDeviceConfiguration(_username, deviceId, new DeviceConfigurationChange(configuration.type, configuration.caption));
		} catch (RetrofitError e) {
			Log.e("gpodder.client", e.getMessage());
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
		}
	}

	public Changes getSubscriptionChanges(String deviceId, int lastCheck) {
		if (!verifyCurrentConfig())
			return null;
		try {
			return _service.getSubscriptionUpdates(_username, deviceId, lastCheck);
		} catch (RetrofitError e) {
			Log.e("gpodder.client", e.getMessage());
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
			return null;
		}
	}

	public void syncSubscriptionDiffs(String deviceId) {
		if (!verifyCurrentConfig())
			return;

		List<String> toAdd = PodaxDB.gPodder.getToAdd();
		List<String> toRemove = PodaxDB.gPodder.getToRemove();

		if (toAdd.size() == 0 && toRemove.size() == 0)
			return;

		try {
			_service.uploadSubscriptionChanges(_username, deviceId, new SubscriptionChanges(toAdd, toRemove));
		} catch (RetrofitError e) {
			Log.e("gpodder.client", e.getMessage());
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
		}

		PodaxDB.gPodder.clear();
	}

	public EpisodeUpdateConfirmation updateEpisodes(List<EpisodeUpdate> updates) {
		if (!verifyCurrentConfig())
			return null;
		try {
			return _service.uploadEpisodeChanges(_username, updates);
		} catch (RetrofitError e) {
			Log.e("gpodder.client", e.getMessage());
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
			return null;
		}
	}

	public EpisodeUpdateResponse getEpisodeUpdates(long since) {
		if (!verifyCurrentConfig())
			return null;
		try {
			return _service.getEpisodeUpdates(_username, since);
		} catch (RetrofitError e) {
			Log.e("gpodder.client", e.getMessage());
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
			return null;
		}
	}

	public List<Podcast> getAllSubscriptions() {
		if (!verifyCurrentConfig())
			return null;

		try {
			return _service.getAllSubscriptions(_username);
		} catch (RetrofitError e) {
			Log.e("gpodder.client", e.getMessage());
			_errorMessage = _context.getString(R.string.gpodder_sync_error);
			return null;
		}
	}
}
