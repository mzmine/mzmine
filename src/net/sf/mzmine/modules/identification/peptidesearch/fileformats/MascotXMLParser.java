/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */


package net.sf.mzmine.modules.identification.peptidesearch.fileformats;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MascotXMLParser extends DefaultHandler {

	private HashMap<String,Object> massesMap;
	private String modification = null;
	private String attribute = null;
	private double mass = 0.0;

	/**
	 * This class parse the section "unimod" of a Mascot result .dat file.
	 * The unimod section is XML format, that contains information of elements, 
	 * modifications and amino acids (mono_mass, ave_mass, full name, title, etc.)
	 * 
	 * @param xmlString
	 */
	public MascotXMLParser(String xmlString) {

		// Use the default (non-validating) parser
		try {

			massesMap = new HashMap<String,Object>(); 
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			InputStream is = new ByteArrayInputStream(xmlString
					.getBytes("UTF-8"));
			saxParser.parse(is, this);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public HashMap<String, Object> getMassesMap(){
		return massesMap;
	}
	
	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {

		mass = 0.0;
		attribute = null;
		
		// <umod:elem>
		if (qName.equals("umod:elem")) {
			attribute = attrs.getValue("mono_mass");
			if (attribute != null)
				mass = Double.parseDouble(attribute);
			attribute = attrs.getValue("full_name");
			massesMap.put(attribute, mass);
		}

		// <umod:mod>
		if (qName.equals("umod:mod")) {
			modification = attrs.getValue("title");
		}
		// <umod:delta>
		if (qName.equals("umod:delta")) {
			attribute = attrs.getValue("mono_mass");
			if (attribute != null)
				mass = Double.parseDouble(attribute);
			if (modification != null){
				massesMap.put(modification, mass);
			}
		}
		
		// <umod:aa>
		if (qName.equals("umod:aa")) {
			attribute = attrs.getValue("mono_mass");
			if (attribute != null)
				mass = Double.parseDouble(attribute);
			attribute = attrs.getValue("title");
			massesMap.put(attribute, mass);
		}

	}

	/**
	 * endElement()
	 */
	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
	) throws SAXException {

		// <umod:mod>
		if (qName.equals("umod:mod")) {
			modification = null;
		}
	}

	/**
	 * characters()
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char buf[], int offset, int len) throws SAXException {
		
	}

	
	

}
