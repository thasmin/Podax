package com.axelby.gpodder;

public class DeviceConfiguration {
	private final String _caption;
	private final String _type;

	public DeviceConfiguration(String caption, String type) {
		_caption = caption;
		_type = type;
	}

	public String getCaption() {
		return _caption;
	}

	public String getType() {
		return _type;
	}
}
