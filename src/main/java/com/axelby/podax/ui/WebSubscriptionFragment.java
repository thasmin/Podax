package com.axelby.podax.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.axelby.podax.R;
import com.axelby.podax.RSSUrlFinderService;
import com.axelby.podax.SubscriptionProvider;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;

public class WebSubscriptionFragment extends Fragment {
    private final int LOADER_GETCODE = 0;
    private final int LOADER_CHECKFORURL = 1;

    private ProgressBar _progressBar;
    private TextView _webcode;

    static class MaybeString {
        private MaybeString(String r, String e) { result = r; error = e; }
        public static MaybeString result(String result) { return new MaybeString(result, null); }
        public static MaybeString error(String error) { return new MaybeString(null, error); }
        public final String result;
        public final String error;
    }

    private final LoaderManager.LoaderCallbacks<MaybeString> _loaderCallbacks = new LoaderManager.LoaderCallbacks<MaybeString>() {
        @Override
        public Loader<MaybeString> onCreateLoader(int id, Bundle bundle) {
            if (id == LOADER_GETCODE)
                return new WebCodeLoader(getActivity());
            else if (id == LOADER_CHECKFORURL)
                return new UrlCheckerLoader(getActivity());
            return null;
        }

        @Override
        public void onLoadFinished(Loader<MaybeString> loader, MaybeString result) {
            if (loader.getId() == LOADER_GETCODE) {
                if (result.error != null) {
                    setErrorMessage(result.error);
                    return;
                }
                _webcode.setText(result.result);
                _progressBar.setVisibility(View.VISIBLE);
                getLoaderManager().initLoader(LOADER_CHECKFORURL, null, this);
            } else if (loader.getId() == LOADER_CHECKFORURL) {
                if (result.error != null) {
                    setErrorMessage(result.error);
                    return;
                }

                if (!result.result.equals(""))
                    RSSUrlFinderService.findRSSUrl(getActivity(), result.result);
                getLoaderManager().restartLoader(LOADER_CHECKFORURL, null, this);
            }
        }

        @Override public void onLoaderReset(Loader<MaybeString> loader) { }
    };

    private final BroadcastReceiver _rssUrlFinderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(RSSUrlFinderService.ACTION_ERROR)) {
                String reason = intent.getStringExtra(RSSUrlFinderService.EXTRA_REASON);
                if (reason == null || reason.equals(""))
                    return;
                Toast.makeText(getActivity(), "Unable to find feed: " + reason, Toast.LENGTH_SHORT).show();
            } else if (action.equals(RSSUrlFinderService.ACTION_SUCCESS)) {
                String url = intent.getStringExtra(RSSUrlFinderService.EXTRA_URL);
                SubscriptionProvider.addNewSubscription(getActivity(), url);
            }
        }
    };

    public WebSubscriptionFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web_subscription, container, false);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(_rssUrlFinderReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(RSSUrlFinderService.ACTION_ERROR);
        intentFilter.addAction(RSSUrlFinderService.ACTION_SUCCESS);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(_rssUrlFinderReceiver, intentFilter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        _webcode = (TextView) getActivity().findViewById(R.id.web_code);
        _progressBar = (ProgressBar) getActivity().findViewById(R.id.progress);

        getLoaderManager().initLoader(LOADER_GETCODE, null, _loaderCallbacks);
    }

    private void setErrorMessage(@Nonnull String errorMessage) {
        if (getActivity() == null)
            return;

        _progressBar.setVisibility(View.GONE);
        _webcode.setVisibility(View.GONE);
        getActivity().findViewById(R.id.your_code_is).setVisibility(View.GONE);

        TextView error = (TextView) getActivity().findViewById(R.id.error);
        error.setVisibility(View.VISIBLE);
        error.setText(errorMessage);
    }

    private abstract static class ATLoader<D> extends AsyncTaskLoader<D> {
        D _result;

        public ATLoader(Context context) {
            super(context);
        }

        @Override
        public void deliverResult(D data) {
            if (isReset())
                return;
            this._result = data;
            super.deliverResult(data);
        }

        // necessary to get asynctaskloader to work
        @Override protected void onStartLoading() {
            if (!takeContentChanged() && _result != null) {
                deliverResult(_result);
            }
            if (takeContentChanged() || _result == null) {
                forceLoad();
            }
        }

        @Override protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            _result = null;
        }
    }

    private static class WebCodeLoader extends ATLoader<MaybeString> {
        public WebCodeLoader(Context context) {
            super(context);
        }

        @Override
        public MaybeString loadInBackground() {
            HttpsURLConnection conn = null;
            try {
                URL url = new URL("https://www.podaxapp.com/web_subscribe/get_code");
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "podax/" + getContext().getResources().getString(R.string.app_version));
                conn.connect();

                int code = conn.getResponseCode();
                if (code != 200) {
                    byte[] b = new byte[1024];
                    InputStream stream = conn.getErrorStream();
                    stream.read(b);
                    stream.close();
                    return MaybeString.error("Server Error: " + new String(b));
                }

                JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
                reader.beginArray();
                String webcode = reader.nextString();
                reader.endArray();
                return MaybeString.result(webcode);
            } catch (MalformedURLException e) {
                return MaybeString.error("Malformed URL: " + e.getMessage());
            } catch (ProtocolException e) {
                return MaybeString.error("Protocol Exception: " + e.getMessage());
            } catch (IOException e) {
                return MaybeString.error("IO Exception: " + e.getMessage());
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        }

    }

    private static class UrlCheckerLoader extends ATLoader<MaybeString> {
        public UrlCheckerLoader(Context context) {
            super(context);
        }

        @Override
        public MaybeString loadInBackground() {
            HttpsURLConnection conn = null;
            try {
                try {
                    Thread.sleep(4000, 0);
                } catch (InterruptedException ignored) { }

                URL url = new URL("https://www.podaxapp.com/web_subscribe/url_grab");
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "podax/" + getContext().getResources().getString(R.string.app_version));
                conn.connect();

                int code = conn.getResponseCode();
                if (code != 200) {
                    byte[] b = new byte[1024];
                    InputStream stream = conn.getErrorStream();
                    stream.read(b);
                    stream.close();
                    return MaybeString.error("Server Error: " + new String(b));
                }

                JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
                reader.beginArray();
                String webcode = reader.nextString();
                reader.endArray();
                return MaybeString.result(webcode);
            } catch (MalformedURLException e) {
                return MaybeString.error("Malformed URL: " + e.getMessage());
            } catch (ProtocolException e) {
                return MaybeString.error("Protocol Exception: " + e.getMessage());
            } catch (IOException e) {
                return MaybeString.error("IO Exception: " + e.getMessage());
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        }
    }
}
