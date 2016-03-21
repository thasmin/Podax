package com.axelby.riasel;

import java.util.Date;

public class FeedItem {

	private String _title;
	private String _paymentURL;
	private String _link;
	private String _description;
	private Date _publicationDate;
	private String _mediaURL;
	private Long _mediaSize;

	public String getTitle() {
		return _title;
	}
	public void setTitle(String title) {
		this._title = title;
	}

	public String getPaymentURL() {
		return _paymentURL;
	}
	public void setPaymentURL(String paymentURL) {
		this._paymentURL = paymentURL;
	}

	public String getLink() {
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

	public Long getMediaSize() {
		return _mediaSize;
	}
	public void setMediaSize(Long mediaSize) {
		this._mediaSize = mediaSize;
	}

}
