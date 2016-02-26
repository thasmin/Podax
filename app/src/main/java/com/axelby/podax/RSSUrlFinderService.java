package com.axelby.podax;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class RSSUrlFinderService extends IntentService {
    public static final String ACTION_ERROR = "com.axelby.podax.RSSFINDER_ERROR";
    public static final String ACTION_SUCCESS = "com.axelby.podax.RSSFINDER_SUCCESS";
    public static final String EXTRA_REASON = "com.axelby.podax.reason";
    public static final String EXTRA_URL = "com.axelby.podax.url";

    public static void findRSSUrl(Context context, String startUrl) {
        Intent intent = new Intent(context, RSSUrlFinderService.class);
        intent.putExtra(RSSUrlFinderService.EXTRA_URL, startUrl);
        context.startService(intent);
    }

    public RSSUrlFinderService() {
        super("RSSUrlFinderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        String urlStr = intent.getStringExtra(RSSUrlFinderService.EXTRA_URL);
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://"))
            urlStr = "http://" + urlStr;

        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            sendErrorBroadcast("Malformed URL");
            return;
        }

        String body;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            body = response.body().string();
        } catch (IOException e) {
            sendErrorBroadcast(e.getMessage());
            return;
        }

        String tagName = findFirstTagName(body);
        if (tagName.equals("rss") || tagName.equals("feed")) {
            sendSuccessBroadcast(urlStr);
            return;
        }

        if (!tagName.equals("html")) {
            sendErrorBroadcast("Invalid document - not HTML or RSS");
            return;
        }

        // look for <links that are have type rss and return the first
        int at = 0;
        while (true) {
            int linkStart = body.indexOf("<link", at);
            if (linkStart == -1)
                break;
            int linkEnd = body.indexOf('>', linkStart);
            String tag = body.substring(linkStart, linkEnd+1);
            at = linkEnd + 1;

            // ensure type is rss or atom
            int typeStart = tag.indexOf("type=\"");
            if (typeStart == -1)
                continue;
            int typeEnd = tag.indexOf('"', typeStart + 6);
            String type = tag.substring(typeStart+6, typeEnd);
            if (!type.contains("rss") && !type.contains("atom"))
                continue;

            int hrefStart = tag.indexOf("href=\"");
            if (hrefStart == -1)
                continue;
            int hrefEnd = tag.indexOf('"', hrefStart + 6);
            String href = tag.substring(hrefStart + 6, hrefEnd);

            sendSuccessBroadcast(href);
            return;
        }

        sendErrorBroadcast("Unable to find RSS feed");
   }

    private void sendSuccessBroadcast(String url) {
        Intent successIntent = new Intent(ACTION_SUCCESS);
        successIntent.putExtra(RSSUrlFinderService.EXTRA_URL, url);
        LocalBroadcastManager.getInstance(this).sendBroadcast(successIntent);
    }

    private void sendErrorBroadcast(String reason) {
        Intent errorIntent = new Intent(ACTION_ERROR);
        errorIntent.putExtra(EXTRA_REASON, reason);
        LocalBroadcastManager.getInstance(this).sendBroadcast(errorIntent);
    }

    private String findFirstTagName(String body) {
        // find first tag that's not the xml specification and not the doctype
        // i'm sure there's a better way to do this
        int at = 0;
        while (body.charAt(at) != '<' || body.charAt(at+1) == '?' || body.charAt(at+1) == '!')
            at = body.indexOf('<', at+1);
        int start = at = at + 1;
        while (Character.isLetter(body.charAt(at)))
            at += 1;
        return body.substring(start, at);
    }
}
