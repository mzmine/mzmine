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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class ProjectSerializer {

	private ZipOutputStream zipOutputStream;
	private ZipInputStream zipInputStream;

	public ProjectSerializer(ZipOutputStream zipStream) {
		this.zipOutputStream = zipStream;
	}

	public ProjectSerializer(ZipInputStream zipStream) {
		this.zipInputStream = zipStream;
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

	public void saveProjectDescription(MZmineProjectImpl project) throws IOException {
		StoredProjectDescription description = new StoredProjectDescription(project);
		zipOutputStream.putNextEntry(new ZipEntry("info"));
		ObjectOutputStream oos = new ObjectOutputStream(zipOutputStream);
		oos.writeObject(description);
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
}
