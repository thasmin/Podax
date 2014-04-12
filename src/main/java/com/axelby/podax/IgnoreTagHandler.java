package com.axelby.podax;

import android.text.Editable;
import android.text.Html;

import org.xml.sax.XMLReader;

public class IgnoreTagHandler implements Html.TagHandler {
	@Override
	public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
	}
}
