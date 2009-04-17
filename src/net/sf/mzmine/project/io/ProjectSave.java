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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class ProjectSave {

	private ZipOutputStream zipOutputStream;

	public ProjectSave(ZipOutputStream zipStream) {
		this.zipOutputStream = zipStream;
	}

	public void saveConfiguration() throws IOException {
		zipOutputStream.putNextEntry(new ZipEntry("configuration.xml"));
		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
		MZmineCore.saveConfiguration(tempConfigFile);
		FileInputStream fileStream = new FileInputStream(tempConfigFile);
		ReadableByteChannel in = Channels.newChannel(fileStream);
		WritableByteChannel out = Channels.newChannel(zipOutputStream);

		ByteBuffer bbuffer = ByteBuffer.allocate(65536);

		while (in.read(bbuffer) != -1) {
			bbuffer.flip();
			out.write(bbuffer);
			bbuffer.clear();
		}
		in.close();
		
		tempConfigFile.delete();
	}

	public void saveProjectDescription(MZmineProjectImpl project) throws IOException {
		Document document = this.saveProjectInformation(project);

		zipOutputStream.putNextEntry(new ZipEntry("Project description.xml"));
		OutputStream finalStream = zipOutputStream;
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(finalStream, format);
		writer.write(document);
	}

	private Document saveProjectInformation(MZmineProjectImpl project) {
		Element newElement;
		Document document = DocumentFactory.getInstance().createDocument();
		Element saveRoot = document.addElement("project");

		// <RAWDATAFILES>
		newElement = saveRoot.addElement("rawdatafiles");
		newElement.addAttribute("quantity", String.valueOf(project.getDataFiles().length));
		this.fillRawDataNames(newElement, project);

		// <NUM_PEAKLISTS>
		newElement = saveRoot.addElement("peaklists");
		newElement.addAttribute("quantity", String.valueOf(project.getPeakLists().length));
		this.fillPeakListNames(newElement, project);

		return document;
	}

	private void fillRawDataNames(Element element, MZmineProjectImpl project) {
		Element newElement;
		RawDataFile[] dataFiles = project.getDataFiles();
		for (int i = 0; i< dataFiles.length; i++) {
			newElement = element.addElement("rawdata");
			newElement.addAttribute("id", String.valueOf(i));
			newElement.addText(dataFiles[i].getName());
		}
	}

	private void fillPeakListNames(Element element, MZmineProjectImpl project) {
		Element newElement;
		PeakList[] peakLists = project.getPeakLists();
		for (int i = 0; i < peakLists.length; i++) {
			newElement = element.addElement("peaklist");
			newElement.addAttribute("id", String.valueOf(i));
			newElement.addText(peakLists[i].getName());
		}
	}
}
