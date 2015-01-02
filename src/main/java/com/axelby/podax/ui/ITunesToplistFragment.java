package com.axelby.podax.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class ITunesToplistFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<List<ITunesToplistFragment.ITunesPodcast>> {

    private ITunesToplistAdapter _adapter;
    private ProgressDialogFragment _progressDialog;
	private RecyclerView _listView;

	public static class ITunesPodcast {
        public long id;
        public String name;
        public String summary;
        public String imageUrl;
        public String idUrl;

        @Override public String toString() { return name; }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long categoryId = getArguments().getLong(Constants.EXTRA_CATEGORY_ID, -1L);
        if (categoryId == -1L)
            throw new IllegalArgumentException("invalid itunes podcast category id");
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recyclerview, container, false);
    }

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (RecyclerView) view.findViewById(R.id.recyclerview);
		_listView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		_listView.setItemAnimator(new DefaultItemAnimator());
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        _adapter = new ITunesToplistAdapter(getActivity());
        _listView.setAdapter(_adapter);
    }

    @Override
    public Loader<List<ITunesPodcast>> onCreateLoader(int i, Bundle bundle) {
        if (i != 0)
            return null;

        _progressDialog = ProgressDialogFragment.newInstance();
        _progressDialog.setMessage("Asking iTunes for list...");
        _progressDialog.show(getActivity().getSupportFragmentManager(), "progress");

        return new ITunesPodcastLoader(getActivity(), bundle.getLong(Constants.EXTRA_CATEGORY_ID));
    }

    private final Handler _handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 206)
                _progressDialog.dismiss();
        }
    };

    @Override
    public void onLoadFinished(Loader<List<ITunesPodcast>> loader, List<ITunesPodcast> podcasts) {
		_adapter.setPodcasts(podcasts);
        _handler.sendEmptyMessage(206);
    }

    @Override
    public void onLoaderReset(Loader<List<ITunesPodcast>> loader) {
        _adapter.setPodcasts(null);
    }

    private class ITunesToplistAdapter extends RecyclerView.Adapter<ITunesToplistAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView summary;
            final NetworkImageView image;
            final View subscribe;

			private final View.OnClickListener _expandSummaryListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (summary.getEllipsize() != null) {
						summary.setMaxLines(Integer.MAX_VALUE);
						summary.setEllipsize(null);
					}
				}
			};

			private final View.OnClickListener _subscribeListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					final Handler handler = new Handler();
					final Context context = view.getContext();

					_progressDialog = ProgressDialogFragment.newInstance();
					_progressDialog.setMessage("Retrieving RSS from iTunes...");
					_progressDialog.show(getActivity().getSupportFragmentManager(), "progress");

					final String url = (String) view.getTag();
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
								connection.setRequestProperty("User-Agent", "iTunes/10.2.1");
								BufferedInputStream stream = new BufferedInputStream(connection.getInputStream());
								byte[] bytes = new byte[100000];
								int read = stream.read(bytes);
								stream.close();
								if (read == 0)
									return;

								String resp = new String(bytes);
								int doctypeAt = resp.indexOf("<!DOCTYPE ");
								String doctype = resp.substring(doctypeAt + 10, resp.indexOf(" ", doctypeAt + 10));
								if (doctype.equals("plist")) {
									int urlAt = resp.indexOf("<key>url</key>");
									int urlStart = resp.indexOf(">", urlAt + 15) + 1;
									int urlEnd = resp.indexOf("<", urlStart);
									String newUrl = resp.substring(urlStart, urlEnd).replace("&amp;", "&");

									connection = (HttpURLConnection) new URL(newUrl).openConnection();
									connection.setRequestProperty("User-Agent", "iTunes/10.2.1");
									stream = new BufferedInputStream(connection.getInputStream());
									read = stream.read(bytes);
									stream.close();
									if (read == 0)
										return;
									resp = new String(bytes);
								}

								int rssUrlStart = resp.indexOf("feed-url=\"") + 10;
								int rssUrlEnd = resp.indexOf("\"", rssUrlStart);
								String rssUrl = resp.substring(rssUrlStart, rssUrlEnd);

								SubscriptionProvider.addNewSubscription(context, rssUrl);
							} catch (IOException e) {
								Log.e("Podax", "error retrieving rss url from iTunes", e);
							} finally {
								handler.post(new Runnable() {
									@Override
									public void run() {
										_progressDialog.dismiss();
									}
								});
							}
						}
					}).start();
				}
			};

			public ViewHolder(View view) {
				super(view);

				name = (TextView) view.findViewById(R.id.name);
                summary = (TextView) view.findViewById(R.id.summary);
                image = (NetworkImageView) view.findViewById(R.id.image);
                subscribe = view.findViewById(R.id.subscribe);

				view.setOnClickListener(_expandSummaryListener);
				subscribe.setOnClickListener(_subscribeListener);
            }
        }

        final ImageLoader _imageLoader;
		List<ITunesPodcast> _podcasts = null;

        public ITunesToplistAdapter(Context context) {
            _imageLoader = Helper.getImageLoader(context);
        }

		public void setPodcasts(List<ITunesPodcast> podcasts) {
			_podcasts = podcasts;
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			if (_podcasts == null)
				return 0;
			return _podcasts.size();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_itunes_toplist_item, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			holder.summary.setMaxLines(3);
			holder.summary.setEllipsize(TextUtils.TruncateAt.END);

            ITunesPodcast podcast = _podcasts.get(position);
            holder.name.setText(podcast.name);
            holder.summary.setText(podcast.summary);
            holder.image.setImageUrl(podcast.imageUrl, _imageLoader);
            holder.subscribe.setTag(podcast.idUrl);
        }

        @Override
        public long getItemId(int position) {
            return _podcasts.get(position).id;
        }
    }

    private static class ITunesPodcastLoader extends AsyncTaskLoader<List<ITunesPodcast>> {
        final long _category;
        List<ITunesPodcast> _lastResult;

        public ITunesPodcastLoader(@Nonnull Context context, long category) {
            super(context);
            _category = category;
        }

        @Override
        public List<ITunesPodcast> loadInBackground() {
            StringBuilder url = new StringBuilder();
            url.append("https://itunes.apple.com/us/rss/toppodcasts/limit=100/");
            if (_category != 0) {
                url.append("genre=");
                url.append(_category);
                url.append("/");
            }
            url.append("explicit=true/xml");

            List<ITunesPodcast> podcasts = new ArrayList<>(100);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new URL(url.toString()).openStream(), "utf-8");

                // find feed tag
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.START_TAG)
                    eventType = parser.next();

                // find entry tag
                for (eventType = parser.next(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next())
                    if (eventType == XmlPullParser.START_TAG && isAtomElement(parser, "entry"))
                        podcasts.add(handleEntry(parser));
            } catch (IOException e) {
                Log.e("Podax", "error loading itunes toplist", e);
                return null;
            } catch (XmlPullParserException e) {
                Log.e("Podax", "error loading itunes toplist", e);
                return null;
            }

            return podcasts;
        }

        @Override
        public void deliverResult(List<ITunesPodcast> data) {
            if (isReset()) {
                _lastResult = null;
                return;
            }

            if (isStarted())
                super.deliverResult(data);

            if (_lastResult != null && _lastResult != data)
                _lastResult = data;
        }

        @Override
        protected void onStartLoading() {
            if (takeContentChanged() || _lastResult == null)
                forceLoad();
            else
                deliverResult(_lastResult);
        }

        @Override
        public void onCanceled(List<ITunesPodcast> data) {
            super.onCanceled(data);
            _lastResult = null;
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
            _lastResult = null;
        }

        private ITunesPodcast handleEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
            ITunesPodcast podcast = new ITunesPodcast();
            for (int eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (isAtomElement(parser, "id")) {
                        podcast.id = Long.valueOf(parser.getAttributeValue(NS_ITUNES, "id"));
                        podcast.idUrl = parser.nextText();
                    } else if (isAtomElement(parser, "summary")) {
                        podcast.summary = parser.nextText();
                    } else if (isITunesElement(parser, "name")) {
                        podcast.name = parser.nextText();
                    } else if (isITunesElement(parser, "image")
                            && parser.getAttributeValue("", "height").equals("170")) {
                        podcast.imageUrl = parser.nextText().replace("170", "100");
                    }
                } else if (eventType == XmlPullParser.END_TAG && isAtomElement(parser, "entry")) {
                    return podcast;
                }
            }
            return podcast;
        }

        private static final String NS_ATOM = "http://www.w3.org/2005/Atom";
        private static final String NS_ITUNES = "http://itunes.apple.com/rss";
        private static boolean isITunesElement(@Nonnull XmlPullParser parser, @Nonnull String name) {
            return name.equals(parser.getName()) && NS_ITUNES.equals(parser.getNamespace());
        }
        private static boolean isAtomElement(@Nonnull XmlPullParser parser, @Nonnull String name) {
            return name.equals(parser.getName()) && NS_ATOM.equals(parser.getNamespace());
        }
    }
}
