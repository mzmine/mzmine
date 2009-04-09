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

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class PeakListSerializer {

	private ZipOutputStream zipOutputStream;
	private ZipInputStream zipInputStream;
	private PeakList buildingPeakList;
	PeakListOpen peakListOpen;
	PeakListSave peakListSave;

	public PeakListSerializer(ZipOutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;

	}

	public PeakListSerializer(ZipInputStream zipInputStream) {
		this.zipInputStream = zipInputStream;
	}

	public double getProgress() {
		if (peakListOpen != null) {
			return peakListOpen.getProgress();
		} else if (peakListSave != null) {
			return peakListSave.getProgress();
		} else {
			return 0.0;
		}
	}

	public void savePeakList(PeakList peakList) throws IOException {
		peakListSave = new PeakListSave();
		peakListSave.savePeakList(peakList);
		Document document = peakListSave.getDocument();

		if (document == null) {
			return;
		}
		zipOutputStream.putNextEntry(new ZipEntry(peakList.getName()));
		OutputStream finalStream = zipOutputStream;
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(finalStream, format);
		writer.write(document);
	}

	public void readPeakList() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			zipInputStream.getNextEntry();
			peakListOpen = new PeakListOpen();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new UnclosableInputStream(zipInputStream), peakListOpen);
			buildingPeakList = peakListOpen.getPeakList();

		} catch (Throwable e) {
			e.printStackTrace();
			return;
		}

		if (buildingPeakList == null || buildingPeakList.getNumberOfRows() == 0) {
			return;
		}
		// Add new peaklist to the project or MZviewer.desktop
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(buildingPeakList);
	}
}