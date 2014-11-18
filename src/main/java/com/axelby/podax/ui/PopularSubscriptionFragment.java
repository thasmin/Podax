package com.axelby.podax.ui;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.support.v4.app.Fragment;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

import java.io.IOException;

public class PopularSubscriptionFragment extends Fragment {

	private class FeedDetails {
		private String title;
		private String description;

		private FeedDetails() {
		}
	}

	ProgressDialog _dialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.popularsubscription, container, false);
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		String title = getArguments().getString(Constants.EXTRA_TITLE);
		TextView titleView = (TextView) getActivity().findViewById(R.id.title);
		titleView.setText(title);

		Button add_subscription = (Button) getActivity().findViewById(R.id.add_subscription);
		add_subscription.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
                SubscriptionProvider.addNewSubscription(getActivity(), getArguments().getString(Constants.EXTRA_URL));
				getFragmentManager().popBackStack();
			}
		});

		_dialog = ProgressDialog.show(getActivity(), "", "Loading Subscription...", true, true);

		String url = getArguments().getString(Constants.EXTRA_URL);
		new AsyncTask<String, Void, FeedDetails>() {
			@Override
			protected FeedDetails doInBackground(String... urls) {
				if (urls.length != 1)
					return null;

				final FeedDetails details = new FeedDetails();

				RootElement root = new RootElement("rss");
				Element channel = root.getChild("channel");
				channel.getChild("title").setEndTextElementListener(new EndTextElementListener() {
					public void end(String body) {
						details.title = body;
					}
				});
				channel.getChild("description").setEndTextElementListener(new EndTextElementListener() {
					public void end(String body) {
						details.description = body;
					}
				});

				HttpGet get = new HttpGet(urls[0]);
				HttpClient client = new DefaultHttpClient();
				try {
					HttpResponse response = client.execute(get);
					Xml.parse(response.getEntity().getContent(), Xml.Encoding.UTF_8, root.getContentHandler());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}

				return details;
			}

			@Override
			protected void onPostExecute(FeedDetails result) {
				TextView title = (TextView) getActivity().findViewById(R.id.title);
				title.setText(result.title);

				TextView description = (TextView) getActivity().findViewById(R.id.description);
				description.setText(result.description);

				_dialog.dismiss();
			}

		}.execute(url);
	}
}
