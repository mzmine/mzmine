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

import net.sf.mzmine.project.impl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectSerializer extends DefaultHandler {

	private ZipOutputStream zipOutputStream;
	private ZipInputStream zipInputStream;
	private StringBuffer charBuffer;
	private int numRawDataFiles,  numPeakLists;
	private String[] rawDataNames,  peakListNames;
	private int rawDataCont,  peakListCont;

	public ProjectSerializer(ZipOutputStream zipStream) {
		this.zipOutputStream = zipStream;
	}

	public ProjectSerializer(ZipInputStream zipStream) {
		this.zipInputStream = zipStream;
		charBuffer = new StringBuffer();
	}

	public void saveConfiguration() throws IOException {
		zipOutputStream.putNextEntry(new ZipEntry("config.xml"));
		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
		MZmineCore.saveConfiguration(tempConfigFile);
		FileInputStream configInputStream = new FileInputStream(tempConfigFile);
		byte buffer[] = new byte[1 << 10]; // 1 MB buffer
		int len;
		while ((len = configInputStream.read(buffer)) > 0) {
			zipOutputStream.write(buffer, 0, len);
		}
		configInputStream.close();
		tempConfigFile.delete();
	}

	public void openConfiguration() {
		try {
			zipInputStream.getNextEntry();

			File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
			FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
			int len;
			byte buffer[] = new byte[1 << 10]; // 1 MB buffer
			while ((len = zipInputStream.read(buffer)) > 0) {
				fileStream.write(buffer, 0, len);
			}
			fileStream.close();
			MZmineCore.loadConfiguration(tempConfigFile);
			tempConfigFile.delete();
		} catch (IOException ex) {
			Logger.getLogger(ProjectSerializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void saveProjectDescription(MZmineProjectImpl project) throws IOException {
		Document document = this.saveProjectInformation(project);

		zipOutputStream.putNextEntry(new ZipEntry("project.description"));
		OutputStream finalStream = zipOutputStream;
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(finalStream, format);
		writer.write(document);
	}

	public void openProjectDescription() {
		try {
			zipInputStream.getNextEntry();
			InputStream InputStream = new UnclosableInputStream(zipInputStream);

			charBuffer = new StringBuffer();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(InputStream, this);
		} catch (Exception ex) {
			Logger.getLogger(ProjectSerializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private Document saveProjectInformation(MZmineProjectImpl project) {
		Element newElement;
		Document document = DocumentFactory.getInstance().createDocument();
		Element saveRoot = document.addElement("project");

		// <RAWDATAFILES>
		newElement = saveRoot.addElement("rawdata");
		newElement.addAttribute("quantity", String.valueOf(project.getDataFiles().length));
		this.fillRawDataNames(newElement, project);

		// <NUM_PEAKLISTS>
		newElement = saveRoot.addElement("peaklist");
		newElement.addAttribute("quantity", String.valueOf(project.getPeakLists().length));
		this.fillPeakListNames(newElement, project);

		return document;
	}

	private void fillRawDataNames(Element element, MZmineProjectImpl project) {
		Element newElement;
		for (RawDataFile dataFile : project.getDataFiles()) {
			newElement = element.addElement("rawdata_name");
			newElement.addText(dataFile.getName());
		}
	}

	private void fillPeakListNames(Element element, MZmineProjectImpl project) {
		Element newElement;
		for (PeakList dataFile : project.getPeakLists()) {
			newElement = element.addElement("peaklist_name");
			newElement.addText(dataFile.getName());
		}
	}

	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {
		if (qName.equals("rawdata")) {
			numRawDataFiles = Integer.parseInt(attrs.getValue("quantity"));
			rawDataNames = new String[numRawDataFiles];
		}

		if (qName.equals("peaklist")) {
			numPeakLists = Integer.parseInt(attrs.getValue("quantity"));
			peakListNames = new String[numPeakLists];
		}

	}

	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
			) throws SAXException {

		if (qName.equals("rawdata_name")) {
			rawDataNames[rawDataCont++] = getTextOfElement();
		}

		if (qName.equals("peaklist_name")) {
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
