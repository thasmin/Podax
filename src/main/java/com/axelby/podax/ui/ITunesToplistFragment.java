package com.axelby.podax.ui;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class ITunesToplistFragment
        extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<ITunesToplistFragment.ITunesPodcast>> {

    private ITunesToplistAdapter _adapter;
    private ProgressDialogFragment _progressDialog;

    public static class ITunesPodcast {
        public long id;
        public String name;
        public String summary;
        public String imageUrl;

        /*
        public ITunesPodcast(XPath xPath, Node entry) throws XPathExpressionException {
            id = ((Number) xPath.evaluate("atom:id/@im:id", entry, XPathConstants.NUMBER)).longValue();
            name = (String) xPath.evaluate("im:name", entry, XPathConstants.STRING);
            summary = (String) xPath.evaluate("atom:summary", entry, XPathConstants.STRING);
            imageUrl = (String) xPath.evaluate("im:image[@height=170]", entry, XPathConstants.STRING);
            imageUrl = imageUrl.replace("170", "100");
        }
        */
        public ITunesPodcast() { }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long categoryId = getArguments().getLong(Constants.EXTRA_CATEGORY_ID, -1L);
        if (categoryId == -1L)
            throw new IllegalArgumentException("invalid itunes podcast category id");
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    /*
    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_itunes_toplist, container, false);
    }
    */

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        _adapter = new ITunesToplistAdapter(getActivity());
        setListAdapter(_adapter);
    }

    @Override
    public Loader<List<ITunesPodcast>> onCreateLoader(int i, Bundle bundle) {
        if (i != 0)
            return null;

        _progressDialog = ProgressDialogFragment.newInstance();
        _progressDialog.setMessage("Asking iTunes for list...");
        _progressDialog.show(getActivity().getFragmentManager(), "progress");

        return new ITunesPodcastLoader(getActivity(), bundle.getLong(Constants.EXTRA_CATEGORY_ID));
    }

    private Handler _handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 206)
                _progressDialog.dismiss();
        }
    };

    @Override
    public void onLoadFinished(Loader<List<ITunesPodcast>> loader, List<ITunesPodcast> podcasts) {
        _adapter.addAll(podcasts);
        _handler.sendEmptyMessage(206);
    }

    @Override
    public void onLoaderReset(Loader<List<ITunesPodcast>> loader) {
        _adapter.clear();
    }

    private class ITunesToplistAdapter extends ArrayAdapter<ITunesPodcast> {
        private class ViewHolder {
            TextView name;
            TextView summary;
            NetworkImageView image;

            public ViewHolder(View v) {
                name = (TextView) v.findViewById(R.id.name);
                summary = (TextView) v.findViewById(R.id.summary);
                image = (NetworkImageView) v.findViewById(R.id.image);
            }
        }

        ImageLoader _imageLoader;

        public ITunesToplistAdapter(Context context) {
            super(context, R.layout.fragment_itunes_toplist_item);
            _imageLoader = Helper.getImageLoader(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;

            if (convertView != null) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                view = getActivity().getLayoutInflater().inflate(R.layout.fragment_itunes_toplist_item, parent, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            }

            ITunesPodcast podcast = getItem(position);
            holder.name.setText(podcast.name);
            holder.summary.setText(podcast.summary);
            holder.image.setImageUrl(podcast.imageUrl, _imageLoader);

            return view;
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
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

            List<ITunesPodcast> podcasts = new ArrayList<ITunesPodcast>(100);
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
        protected void onStopLoading() {
            super.onStopLoading();
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
