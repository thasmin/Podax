package com.axelby.riasel;

import java.util.Date;

import android.content.ContentValues;

public class Feed {

	private String _title;
	private String _thumbnail;
    private String _description;
	private Date _lastBuildDate;
	private Date _pubDate;

	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		if (getTitle() != null)
			values.put("title", getTitle());
		if (getThumbnail() != null)
			values.put("thumbnail", getThumbnail());
        if (getDescription() != null)
            values.put("description", getDescription());
		if (getLastBuildDate() != null)
			values.put("lastBuildDate", getLastBuildDate().getTime());
		if (getPubDate() != null)
			values.put("pubDate", getPubDate().getTime());
		return values;
	}

	String getTitle() {
		return _title;
	}
	public void setTitle(String title) {
		this._title = title;
	}

	String getThumbnail() {
		return _thumbnail;
	}
	public void setThumbnail(String thumbnail) {
		_thumbnail = thumbnail;
	}

    String getDescription() {
        return _description;
    }
    public void setDescription(String description) {
        this._description = description;
    }

    Date getLastBuildDate() {
		return _lastBuildDate;
	}
	public void setLastBuildDate(Date lastBuildDate) {
		this._lastBuildDate = lastBuildDate;
	}

	Date getPubDate() {
		return _pubDate;
	}
	public void setPubDate(Date _pubDate) {
		this._pubDate = _pubDate;
	}

}
