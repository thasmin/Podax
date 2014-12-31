package com.axelby.gpodder.dto;

public class DeviceConfigurationChange {
	public String type;
	public String caption;

	public DeviceConfigurationChange(String type, String caption) {
		this.type = type;
		this.caption = caption;
	}
}
