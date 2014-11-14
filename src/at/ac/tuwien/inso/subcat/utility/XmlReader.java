/* XmlReader.java
 *
 * Copyright (C) 2014 Florian Brosch
 *
  *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Author:
 *       Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.subcat.utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;



public class XmlReader {
	private XMLStreamReader xmlReader;

	public XmlReader (String path) throws FileNotFoundException, XmlReaderException {
		assert (path != null);

		InputStream input = new FileInputStream (path);

		try {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			xmlReader = factory.createXMLStreamReader(input);

			xmlReader.next ();

		} catch (XMLStreamException e) {
			throw new XmlReaderException (e);
		}
	}

	public XmlReader (InputStream input) throws XmlReaderException {
		assert (input != null);

		try {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			xmlReader = factory.createXMLStreamReader(input);

			xmlReader.next ();

		} catch (XMLStreamException e) {
			throw new XmlReaderException (e);
		}
	}

	private void skipSpace () throws XmlReaderException {
		try {
			int eventType = xmlReader.getEventType ();
			while (eventType == XMLEvent.SPACE || eventType == XMLEvent.CHARACTERS) {
				if (xmlReader.getText ().trim().length() != 0) {
					return ;
				}
				eventType = xmlReader.next();
			}
		} catch (XMLStreamException e) {
			throw new XmlReaderException (e);
		}
	}

	public String getText () throws XmlReaderException {
		try {
			StringBuilder builder = new StringBuilder ();

			int eventType = xmlReader.getEventType ();

			while (eventType == XMLEvent.CHARACTERS) {
				builder.append (xmlReader.getText());
				eventType = xmlReader.next();
			}

			return builder.toString ();
		} catch (XMLStreamException e) {
			throw new XmlReaderException (e);
		}
	}

	public boolean acceptStart (String element, boolean doSkipSpace) throws XmlReaderException {
		assert (element != null);

		try {
			if (isStart (element)) {
				xmlReader.next ();
				if (doSkipSpace) {
					skipSpace ();
				}
				return true;
			}

			return false;
		} catch (XMLStreamException e) {
			throw new XmlReaderException (e);
		}
	}

	public void expectStart (String element, boolean doSkipSpace) throws XmlReaderException {
		if (!acceptStart (element, doSkipSpace)) {
			Location location = xmlReader.getLocation ();
			throw new XmlReaderException (location.getLineNumber () + "." + location.getColumnNumber () + ": Expected: <" + element + ">, got " + getCurrentToken ());
		}
	}

	public boolean isStart (String element) throws XmlReaderException {
		return isStart (element, false);
	}

	public boolean isStart (String element, boolean doSkipSpace) throws XmlReaderException {
		assert (element != null);

		if (doSkipSpace) {
			this.skipSpace ();
		}
			
		int eventType = xmlReader.getEventType ();
		if (eventType != XMLEvent.START_ELEMENT) {
			return false;
		}

		String name = xmlReader.getLocalName();
		return element.equals (name);
	}
	
	public boolean acceptEnd (String element, boolean doSkipSpace) throws XmlReaderException {
		assert (element != null);

		try {
			int eventType = xmlReader.getEventType ();
			if (eventType != XMLEvent.END_ELEMENT) {
				return false;
			}

			String name = xmlReader.getLocalName();
			if (element.equals (name)) {
				xmlReader.next ();
				if (doSkipSpace) {
					skipSpace ();
				}
				return true;
			}

			return false;
		} catch (XMLStreamException e) {
			throw new XmlReaderException (e);
		}
	}

	public void expectEnd (String element, boolean doSkipSpace) throws XmlReaderException {
		if (!acceptEnd (element, doSkipSpace)) {
			Location location = xmlReader.getLocation ();
			throw new XmlReaderException (location.getLineNumber () + "." + location.getColumnNumber () + ": Expected: </" + element + ">, got " + getCurrentToken ());
		}
	}

	public void next () throws XmlReaderException {
		try {
			xmlReader.next ();
		} catch (XMLStreamException e) {
			throw new XmlReaderException (e);
		}
	}

	public boolean hasAttribute (String name) {
		assert (name != null);

		return xmlReader.getAttributeValue (null, name) != null;
	}

	public String getAttribute (String name) throws XmlReaderException {
		assert (name != null);

		String val = xmlReader.getAttributeValue (null, name);
		if (val == null) {
			throw new XmlReaderException ("Unknown attribute `" + name + "' in `" + getCurrentToken () + "'");
		}

		return val;
	}

	public String getAttribute (String name, String defaultValue) {
		assert (name != null);
		
		String val = xmlReader.getAttributeValue (null, name);
		if (val == null) {
			return defaultValue;
		}

		return val;
	}
	
	private String getCurrentToken () {
		switch (xmlReader.getEventType()) {
		case XMLEvent.CDATA:
			return "CDATA";

		case XMLEvent.SPACE:
			return "<space>";

		case XMLEvent.CHARACTERS:
			return "<text>";

		case XMLEvent.END_ELEMENT:
			return "</" + xmlReader.getLocalName () + ">";

		case XMLEvent.START_ELEMENT:
			return "<" + xmlReader.getLocalName () + ">";

		case XMLEvent.ATTRIBUTE:
			return "<attribute>";

		case XMLEvent.COMMENT:
			return "<comment>";

		case XMLEvent.DTD:
			return "<dtd>";

		case XMLEvent.END_DOCUMENT:
			return "<end-document>";

		case XMLEvent.ENTITY_DECLARATION:
			return "<entity-declaration>";

		case XMLEvent.ENTITY_REFERENCE:
			return "<entity-reference>";

		case XMLEvent.NAMESPACE:
			return "<namespace>";

		case XMLEvent.NOTATION_DECLARATION:
			return "<notation-declaration>";

		case XMLEvent.PROCESSING_INSTRUCTION:
			return "<processing-instruction>";

		case XMLEvent.START_DOCUMENT:
 			return "<start-document>";

		default:
			return "<unknown>";
		}
	}

	public static boolean isXml (String path, String expectedRootElement) {
		try {
			InputStream input = new FileInputStream (path);
			XMLInputFactory factory = XMLInputFactory.newFactory();
			XMLStreamReader xmlReader = factory.createXMLStreamReader(input);
			int token = xmlReader.next ();

			if (token == XMLEvent.PROCESSING_INSTRUCTION) {
				token = xmlReader.next ();
			}

			while (token == XMLEvent.COMMENT || token == XMLEvent.SPACE) {
				token = xmlReader.next ();
			}
			
			if (token != XMLEvent.START_ELEMENT) {
				return false;
			}
			
			if (!expectedRootElement.equals (xmlReader.getLocalName ())) {
				return false;
			}
			
			return true;
		} catch (IOException e) {
			return false;
		} catch (XMLStreamException e) {
			return false;
		}
	}
}