package com.axelby.riasel;

import java.util.Date;

public class Feed {

	private String _title;
	private String _thumbnail;
    private String _description;
	private String _link;
	private Date _lastBuildDate;
	private Date _pubDate;

	public String getTitle() {
		return _title;
	}
	public void setTitle(String title) {
		this._title = title;
	}

	public String getThumbnail() {
		return _thumbnail;
	}
	public void setThumbnail(String thumbnail) {
		_thumbnail = thumbnail;
	}

    public String getDescription() {
        return _description;
    }
    public void setDescription(String description) {
        this._description = description;
    }

	public String getLink() {
		return _link;
	}
	public void setLink(String link) {
		_link = link;
	}

    public Date getLastBuildDate() {
		return _lastBuildDate;
	}
	public void setLastBuildDate(Date lastBuildDate) {
		this._lastBuildDate = lastBuildDate;
	}

	public Date getPubDate() {
		return _pubDate;
	}
	public void setPubDate(Date _pubDate) {
		this._pubDate = _pubDate;
	}
}
