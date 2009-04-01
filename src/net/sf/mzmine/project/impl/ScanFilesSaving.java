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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sf.mzmine.data.RawDataFile;

public class ScanFilesSaving {

	private ZipOutputStream zipStream;
	private RawDataFile[] rawDataFiles;
	private int scanDataFileBytes;
	public ScanFilesSaving(ZipOutputStream zipStream, RawDataFile[] rawDataFiles) {
		this.zipStream = zipStream;
		this.rawDataFiles = rawDataFiles;
	}

	public void saveScanObjects() throws IOException {
		int cont = 0;
		for(RawDataFile dataFile : rawDataFiles){
			zipStream.putNextEntry(new ZipEntry(dataFile.getName() + "-" + cont++));
			try {
				copyFile(((RawDataFileImpl) dataFile).getScanDataFileasFile(), zipStream);
			} catch (Exception ex) {
				Logger.getLogger(ScanFilesSaving.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

	}

	public int getscansbytes(){
		return this.scanDataFileBytes;
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
}
