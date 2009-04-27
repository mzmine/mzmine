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

package net.sf.mzmine.modules.io.rawdataimport;

import java.io.File;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

public class RawDataImporterParameters extends SimpleParameterSet {

    private File fileNames[];

    public static final Parameter importDirectory = new SimpleParameter(
            ParameterType.STRING, "Last open directory",
            "Last directory used to import files");

    public static final Parameter lastFileNames = new SimpleParameter(
            ParameterType.STRING, "Files to import", "");

    public RawDataImporterParameters() {
        super(new Parameter[] { importDirectory, lastFileNames });
        fileNames = new File[0];
    }

    public File[] getFileNames() {
        return fileNames;
    }

    public void setFileNames(File[] fileNames) {
        this.fileNames = fileNames;

        String files = "  ";
        if (fileNames != null) {
            for (File f : fileNames) {
                files += f.getName() + " ";
            }
        }
        this.setParameterValue(lastFileNames, files);
    }
    
    public RawDataImporterParameters clone() {
		RawDataImporterParameters newParams = (RawDataImporterParameters) super
				.clone();
		if (fileNames != null)
			newParams.fileNames = fileNames.clone();
		return newParams;
	}

}
