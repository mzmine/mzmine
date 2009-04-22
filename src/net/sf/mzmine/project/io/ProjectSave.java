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
import java.util.logging.Level;
import java.util.logging.Logger;
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

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private ZipOutputStream zipOutputStream;

	public ProjectSave(ZipOutputStream zipStream) {
		this.zipOutputStream = zipStream;
	}

	public void saveConfiguration() throws IOException {
		logger.info("Saving configuration file");

		zipOutputStream.putNextEntry(new ZipEntry("configuration.xml"));
		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
		MZmineCore.saveConfiguration(tempConfigFile);
		FileInputStream fileStream = new FileInputStream(tempConfigFile);
		(new SaveFileUtils()).saveFile(fileStream, zipOutputStream, 0, SaveFileUtilsMode.CLOSE_IN);
		tempConfigFile.delete();
	}

	public void saveProjectDescription(MZmineProjectImpl project) throws IOException {
		try {
			logger.info("Saving project information");

			Document document = this.saveProjectInformation(project);

			zipOutputStream.putNextEntry(new ZipEntry("Project description.xml"));
			OutputStream finalStream = zipOutputStream;
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(finalStream, format);

			writer.write(document);
		} catch (Exception ex) {
			MZmineCore.getDesktop().displayErrorMessage("Error saving the project description.");
			Logger.getLogger(ProjectOpeningTask.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private Document saveProjectInformation(MZmineProjectImpl project) {
		Element newElement;
		Document document = DocumentFactory.getInstance().createDocument();
		Element saveRoot = document.addElement("project");

		// <RAWDATAFILES>
		newElement = XMLUtils.fillXMLValues(saveRoot, "rawdatafiles", "quantity", String.valueOf(project.getDataFiles().length), null);
		this.fillRawDataNames(newElement, project);

		// <NUM_PEAKLISTS>
		newElement = XMLUtils.fillXMLValues(saveRoot, "peaklists", "quantity", String.valueOf(project.getPeakLists().length), null);
		this.fillPeakListNames(newElement, project);

		return document;
	}

	private void fillRawDataNames(Element element, MZmineProjectImpl project) {
		RawDataFile[] dataFiles = project.getDataFiles();
		for (int i = 0; i < dataFiles.length; i++) {
			XMLUtils.fillXMLValues(element, "rawdata", "id", String.valueOf(i), dataFiles[i].getName());
		}
	}

	private void fillPeakListNames(Element element, MZmineProjectImpl project) {
		PeakList[] peakLists = project.getPeakLists();
		for (int i = 0; i < peakLists.length; i++) {
			XMLUtils.fillXMLValues(element, "peaklist", "id", String.valueOf(i), peakLists[i].getName());
		}
	}
}
