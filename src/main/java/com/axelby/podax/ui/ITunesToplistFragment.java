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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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

        public ITunesPodcast(XPath xPath, Node entry) throws XPathExpressionException {
            id = ((Number) xPath.evaluate("atom:id/@im:id", entry, XPathConstants.NUMBER)).longValue();
            name = (String) xPath.evaluate("im:name", entry, XPathConstants.STRING);
            summary = (String) xPath.evaluate("atom:summary", entry, XPathConstants.STRING);
            imageUrl = (String) xPath.evaluate("im:image[@height=170]", entry, XPathConstants.STRING);
            imageUrl = imageUrl.replace("170", "100");
        }

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
            url.append("https://itunes.apple.com/us/rss/toppodcasts/limit=10/");
            if (_category != 0) {
                url.append("genre=");
                url.append(_category);
                url.append("/");
            }
            url.append("explicit=true/xml");

            List<ITunesPodcast> podcasts = new ArrayList<ITunesPodcast>(100);
            try {
                XPathFactory factory = XPathFactory.newInstance();
                XPath xPath = factory.newXPath();
                xPath.setNamespaceContext(new NamespaceContext() {
                    @Override
                    public String getNamespaceURI(String s) {
                        if (s.equals("im")) return "http://itunes.apple.com/rss";
                        if (s.equals("atom")) return "http://www.w3.org/2005/Atom";
                        return XMLConstants.DEFAULT_NS_PREFIX;
                    }
                    @Override public String getPrefix(String s) { return s; }
                    @Override public Iterator getPrefixes(String s) { return null; }
                });

                InputSource source = new InputSource(new URL(url.toString()).openStream());
                NodeList nodes = (NodeList) xPath.evaluate("/atom:feed/atom:entry", source, XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); ++i)
                    podcasts.add(new ITunesPodcast(xPath, nodes.item(i)));
            } catch (IOException e) {
                Log.e("Podax", "error loading itunes toplist", e);
                return null;
            } catch (XPathExpressionException e) {
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
    }
}
