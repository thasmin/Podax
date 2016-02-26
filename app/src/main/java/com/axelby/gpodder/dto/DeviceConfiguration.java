package com.axelby.gpodder.dto;

public class DeviceConfiguration {
	public String id;
	public String type;
	public String caption;
	public int subscriptions;

	public DeviceConfiguration() { }

	public DeviceConfiguration(String type, String caption) {
		this.type = type;
		this.caption = caption;
	}
}

