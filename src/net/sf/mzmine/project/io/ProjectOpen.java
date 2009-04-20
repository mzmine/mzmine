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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.project.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.main.MZmineCore;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectOpen extends DefaultHandler {

	private ZipInputStream zipInputStream;
	private ZipFile zipFile;
	private StringBuffer charBuffer;
	private int numRawDataFiles,  numPeakLists;
	private String[] rawDataNames,  peakListNames;
	private int rawDataCont,  peakListCont;

	public ProjectOpen(ZipInputStream zipStream, ZipFile zipFile) {
		this.zipInputStream = zipStream;
		this.zipFile = zipFile;
		charBuffer = new StringBuffer();
	}

	public void openConfiguration() {
		try {
			zipInputStream.getNextEntry();
			File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
			FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
			(new SaveFileUtils()).saveFile(zipInputStream, fileStream, 0, SaveFileUtilsMode.CLOSE_OUT);
			fileStream.close();
			MZmineCore.loadConfiguration(tempConfigFile);
			tempConfigFile.delete();
		} catch (IOException ex) {
			Logger.getLogger(ProjectOpen.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void openProjectDescription() {
		try {			
			InputStream InputStream = zipFile.getInputStream(zipInputStream.getNextEntry());
			charBuffer = new StringBuffer();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(InputStream, this);
		} catch (Exception ex) {
			Logger.getLogger(ProjectOpen.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {
		if (qName.equals("rawdatafiles")) {
			numRawDataFiles = Integer.parseInt(attrs.getValue("quantity"));
			rawDataNames = new String[numRawDataFiles];
		}

		if (qName.equals("peaklists")) {
			numPeakLists = Integer.parseInt(attrs.getValue("quantity"));
			peakListNames = new String[numPeakLists];
		}

	}

	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
			) throws SAXException {

		if (qName.equals("rawdata")) {
			rawDataNames[rawDataCont++] = getTextOfElement();
		}

		if (qName.equals("peaklist")) {
			peakListNames[peakListCont++] = getTextOfElement();
		}
	}

	/**
	 * Return a string without tab an EOF characters
	 *
	 * @return String element text
	 */
	private String getTextOfElement() {
		String text = charBuffer.toString();
		text = text.replaceAll("[\n\r\t]+", "");
		text = text.replaceAll("^\\s+", "");
		charBuffer.delete(0, charBuffer.length());
		return text;
	}

	/**
	 * characters()
	 *
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char buf[], int offset, int len) throws SAXException {
		charBuffer = charBuffer.append(buf, offset, len);
	}

	public int getNumOfRawDataFiles() {
		return numRawDataFiles;
	}

	public int getNumOfPeakLists() {
		return numPeakLists;
	}

	public String[] getRawDataNames() {
		return rawDataNames;
	}

	public String[] getPeakListNames() {
		return peakListNames;
	}
}
