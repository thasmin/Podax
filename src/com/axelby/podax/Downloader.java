package com.axelby.podax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;

public class Downloader {
	public static JSONObject downloadJSON(String url) {
		try {
			String result = Downloader.downloadString(url);
			if (result == null)
				return null;
			return new JSONObject(result);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String downloadString(String url) {
		InputStream instream = downloadInputStream(url);
		if (instream == null)
			return null;
		return convertStreamToString(instream);
	}
	
	public static InputStream downloadInputStream(String url) {
		try {
			URL u = new URL(url);
			URLConnection c = u.openConnection();
			return c.getInputStream();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
