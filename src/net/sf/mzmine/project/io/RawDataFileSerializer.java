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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class RawDataFileSerializer {

	private ZipOutputStream zipOutputStream;
	private ZipInputStream zipInputStream;
	private DoubleBuffer doubleBuffer;
	RawDataFileOpen rawDataFileOpen;
	RawDataFileSave rawDataFileSave;

	public RawDataFileSerializer(ZipOutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;
	}

	public RawDataFileSerializer(ZipInputStream zipInputStream) {
		this.zipInputStream = zipInputStream;
	}

	public double getProgress() {
		if (rawDataFileOpen != null) {
			return rawDataFileOpen.getProgress();
		} else if (rawDataFileSave != null) {
			return rawDataFileSave.getProgress();
		} else {
			return 0.0;
		}
	}

	public void writeRawDataFiles(RawDataFile rawDataFile) {
		try {

			int cont = 0;
			String newName = rawDataFile.getName() + "-" + cont++;

			zipOutputStream.putNextEntry(new ZipEntry(newName));
			copyFile(((RawDataFileImpl) rawDataFile).getScanDataFileasFile(), zipOutputStream);

			rawDataFileSave = new RawDataFileSave();
			Document document = rawDataFileSave.saveRawDataInformation(rawDataFile);

			zipOutputStream.putNextEntry(new ZipEntry(newName + ".description"));
			OutputStream finalStream = zipOutputStream;
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(finalStream, format);
			writer.write(document);

		} catch (Exception ex) {
			Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void copyFile(File in, ZipOutputStream zipStream) throws Exception {
		FileInputStream fis = new FileInputStream(in);
		try {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				zipStream.write(buffer, 0, bytesRead);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	public void readRawDataFile() throws ClassNotFoundException {
		try {

			// Writes the scan file into a temporal file
			zipInputStream.getNextEntry();
			File tempConfigFile = File.createTempFile("mzmine", ".scans");
			FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
			int cont = 0;
			byte buffer[] = new byte[1 << 10]; // 1 MB buffer
			int len;
			while ((len = zipInputStream.read(buffer)) > 0) {
				fileStream.write(buffer, 0, len);
				cont += len;
			}
			fileStream.close();

			// Creates the new RawDataFile reading the scans from the temporal file and adding them
			// to the RawDataFile
			ByteBuffer bbuffer = ByteBuffer.allocate(cont);
			RandomAccessFile storageFile = new RandomAccessFile(tempConfigFile, "r");
			synchronized (storageFile) {
				try {
					storageFile.seek(0);
					storageFile.read(bbuffer.array(), 0, cont);
				} catch (IOException e) {
				}
			}
			storageFile.close();

			doubleBuffer = bbuffer.asDoubleBuffer();

			// Reads RawDataDescription
			rawDataFileOpen = new RawDataFileOpen(doubleBuffer);

			zipInputStream.getNextEntry();
			InputStream InputStream = new UnclosableInputStream(zipInputStream);

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(InputStream, rawDataFileOpen);

			RawDataFileWriter rawDataFileWriter = rawDataFileOpen.getRawDataFile();

			RawDataFile rawDataFile = rawDataFileWriter.finishWriting();
			MZmineCore.getCurrentProject().addFile(rawDataFile);

			tempConfigFile.delete();
		} catch (Exception ex) {
			Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
