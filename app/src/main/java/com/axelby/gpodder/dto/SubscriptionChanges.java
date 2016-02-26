package com.axelby.gpodder.dto;

import java.util.List;

public class SubscriptionChanges {
	public List<String> add;
	public List<String> remove;

	public SubscriptionChanges(List<String> add, List<String> remove) {
		this.add = add;
		this.remove = remove;
	}
}
