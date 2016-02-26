package com.axelby.riasel;

import android.content.ContentValues;

import java.util.Date;

public class FeedItem {

	private String _uniqueId;
	private String _title;
	private String _paymentURL;
	private String _link;
	private String _description;
	private Date _publicationDate;
	private String _mediaURL;
	private Long _mediaSize;

	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		if (getTitle() != null)
			values.put("title", getTitle());
		if (getPaymentURL() != null)
			values.put("paymentURL", getPaymentURL());
		if (getLink() != null)
			values.put("link", getLink());
		if (getDescription() != null)
			values.put("description", getDescription());
		if (getPublicationDate() != null)
			values.put("publicationDate", getPublicationDate().getTime());
		if (getMediaURL() != null)
			values.put("mediaURL", getMediaURL());
		if (getMediaSize() != null)
			values.put("mediaSize", getMediaSize());
		return values;
	}

	public String getUniqueId() {
		return _uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		_uniqueId = uniqueId;
	}

	String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		this._title = title;
	}

	String getPaymentURL() {
		return _paymentURL;
	}

	public void setPaymentURL(String paymentURL) {
		this._paymentURL = paymentURL;
	}

	String getLink() {
		return _link;
	}

	public void setLink(String link) {
		this._link = link;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		this._description = description;
	}

	public Date getPublicationDate() {
		return _publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this._publicationDate = publicationDate;
	}

	public String getMediaURL() {
		return _mediaURL;
	}

	public void setMediaURL(String mediaURL) {
		this._mediaURL = mediaURL;
	}

	Long getMediaSize() {
		return _mediaSize;
	}

	public void setMediaSize(Long mediaSize) {
		this._mediaSize = mediaSize;
	}

}
