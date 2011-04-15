package com.axelby.podax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class RssHandler extends DefaultHandler {
    //private List<Message> messages;
    //private Message currentMessage;
	private String _lastElement;
	private String _currentElement;
	private String _title;
    private StringBuilder builder;
    
    /*
    public List<Message> getMessages(){
        return this.messages;
    }
    */
    
    public String getTitle() {
		return _title;
	}

	@Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        super.endElement(uri, localName, name);
        //if (this.currentMessage != null){
        if (_lastElement.equalsIgnoreCase("channel") && localName.equalsIgnoreCase("title")) {
            _title = builder.toString().trim();
        	Log.i("Podax", _title);
        }
                /*
            } else if (localName.equalsIgnoreCase("link")){
                currentMessage.setLink(builder.toString());
            } else if (localName.equalsIgnoreCase("description")){
                currentMessage.setDescription(builder.toString());
            } else if (localName.equalsIgnoreCase("pubDate")){
                currentMessage.setDate(builder.toString());
            } else if (localName.equalsIgnoreCase("item")){
                messages.add(currentMessage);
            }
        */
        builder.setLength(0);    
        //}
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        //messages = new ArrayList<Message>();
        builder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, name, attributes);
        _lastElement = _currentElement;
        _currentElement = localName;
    }
}