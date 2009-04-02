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
package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class RawDataFileSerializer {

	private ZipOutputStream zipOutputStream;
	private ZipInputStream zipInputStream;
	private int scanDataFileBytes;

	public RawDataFileSerializer(ZipOutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;
	}

	public RawDataFileSerializer(ZipInputStream zipInputStream) {
		this.zipInputStream = zipInputStream;
	}

	public void writeRawDataFiles(RawDataFile rawDataFile) {
		try {
			int cont = 0;
			String newName = rawDataFile.getName() + "-" + cont++;

			RawDataDescription description = new RawDataDescription(rawDataFile);
			zipOutputStream.putNextEntry(new ZipEntry(newName + ".rawDataDescription"));
			ObjectOutputStream oos = new ObjectOutputStream(zipOutputStream);
			oos.writeObject(description);

			zipOutputStream.putNextEntry(new ZipEntry(newName));
			copyFile(((RawDataFileImpl) rawDataFile).getScanDataFileasFile(), zipOutputStream);

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
				this.scanDataFileBytes += bytesRead;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	public int getscansbytes() {
		return this.scanDataFileBytes;
	}

	public void readRawDataFile() throws ClassNotFoundException {
		try {
			// Reads RawDataDescription
			zipInputStream.getNextEntry();
			ObjectInputStream in = new ObjectInputStream(zipInputStream);
			RawDataDescription rawDataDescription = (RawDataDescription) in.readObject();

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

			DoubleBuffer doubleBuffer = bbuffer.asDoubleBuffer();

			RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(rawDataDescription.getName());
			for (int numScans = 0; numScans < rawDataDescription.getNumOfScans(); numScans++) {
				DataPoint[] dataPoints = new DataPoint[rawDataDescription.getNumOfDataPoints(numScans)];
				for (int j = 0; j < rawDataDescription.getNumOfDataPoints(numScans); j++) {
					dataPoints[j] = new SimpleDataPoint(doubleBuffer.get(), doubleBuffer.get());
				}
				Scan scan = new SimpleScan((RawDataFileImpl) rawDataFileWriter, rawDataDescription.getScanNumber(numScans),
						rawDataDescription.getMsLevel(numScans), rawDataDescription.getRetentionTime(numScans),
						rawDataDescription.getParentScan(numScans), rawDataDescription.getPrecursorMZ(numScans),
						rawDataDescription.getFragmentScans(numScans), dataPoints, rawDataDescription.isCentroided(numScans));
				rawDataFileWriter.addScan(scan);
			}
			RawDataFile rawDataFile = rawDataFileWriter.finishWriting();
			MZmineCore.getCurrentProject().addFile(rawDataFile);

			tempConfigFile.delete();
		} catch (IOException ex) {
			Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
