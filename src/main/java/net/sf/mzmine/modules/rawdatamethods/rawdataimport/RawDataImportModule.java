/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.rawdataimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.AgilentCsvReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NativeFileReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Raw data import module
 */
public class RawDataImportModule implements MZmineProcessingModule {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Raw data import";
    private static final String MODULE_DESCRIPTION = "This module imports raw data into the project.";

    private static final String CDF_HEADER = "CDF";
    private static final String XML_HEADER = "<?xml";
    private static final String MZML_HEADER = "<mzML";
    private static final String MZXML_HEADER = "<mzXML";
    private static final String MZDATA_HEADER = "<mzData";
    private static final String THERMO_HEADER = String.valueOf(new char[] {
	    0x01, 0xA1, 'F', 0, 'i', 0, 'n', 0, 'n', 0, 'i', 0, 'g', 0, 'a', 0,
	    'n', 0 });

    @Override
    public @Nonnull String getName() {
	return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
	return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
	    @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

	File fileNames[] = parameters.getParameter(
		RawDataImportParameters.fileNames).getValue();

	for (int i = 0; i < fileNames.length; i++) {

	    if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
		MZmineCore.getDesktop().displayErrorMessage(
			MZmineCore.getDesktop().getMainWindow(),
			"Cannot read file " + fileNames[i]);
		logger.warning("Cannot read file " + fileNames[i]);
		return ExitCode.ERROR;
	    }

	    RawDataFileWriter newMZmineFile;
	    try {
		newMZmineFile = MZmineCore
			.createNewFile(fileNames[i].getName());
	    } catch (IOException e) {
		MZmineCore.getDesktop().displayErrorMessage(
			MZmineCore.getDesktop().getMainWindow(),
			"Could not create a new temporary file " + e);
		logger.log(Level.SEVERE,
			"Could not create a new temporary file ", e);
		return ExitCode.ERROR;
	    }

	    Task newTask = null;

	    RawDataFileType fileType = detectDataFileType(fileNames[i]);
	    logger.finest("File " + fileNames[i] + " type detected as "
		    + fileType);

	    if (fileType == null) {
		MZmineCore.getDesktop().displayErrorMessage(
			MZmineCore.getDesktop().getMainWindow(),
			"Could not determine the file type of file "
				+ fileNames[i]);
		continue;
	    }

	    switch (fileType) {
	    case MZDATA:
		newTask = new MzDataReadTask(project, fileNames[i],
			newMZmineFile);
		break;
	    case MZML:
		newTask = new MzMLReadTask(project, fileNames[i], newMZmineFile);
		break;
	    case MZXML:
		newTask = new MzXMLReadTask(project, fileNames[i],
			newMZmineFile);
		break;
	    case NETCDF:
		newTask = new NetCDFReadTask(project, fileNames[i],
			newMZmineFile);
		break;
	    case AGILENT_CSV:
		newTask = new AgilentCsvReadTask(project, fileNames[i],
			newMZmineFile);
		break;
	    case THERMO_RAW:
	    case WATERS_RAW:
		newTask = new NativeFileReadTask(project, fileNames[i],
			fileType, newMZmineFile);
	    }

	    if (newTask == null) {
		logger.warning("Cannot determine file type of file "
			+ fileNames[i]);
		return ExitCode.ERROR;
	    }

	    tasks.add(newTask);

	}

	return ExitCode.OK;
    }

    private RawDataFileType detectDataFileType(File fileName) {

	if (fileName.isDirectory()) {
	    // To check for Waters .raw directory, we look for _FUNC[0-9]{3}.DAT
	    for (File f : fileName.listFiles()) {
		if (f.isFile() && f.getName().matches("_FUNC[0-9]{3}.DAT"))
		    return RawDataFileType.WATERS_RAW;
	    }
	    // We don't recognize any other directory type than Waters
	    return null;
	}

	if (fileName.getName().toLowerCase().endsWith(".csv")) {
	    return RawDataFileType.AGILENT_CSV;
	}

	try {
	    InputStreamReader reader = new InputStreamReader(
		    new FileInputStream(fileName), "ISO-8859-1");
	    char buffer[] = new char[512];
	    reader.read(buffer);
	    reader.close();
	    String fileHeader = new String(buffer);

	    if (fileHeader.startsWith(THERMO_HEADER)) {
		return RawDataFileType.THERMO_RAW;
	    }

	    if (fileHeader.startsWith(CDF_HEADER)) {
		return RawDataFileType.NETCDF;
	    }

	    if (fileHeader.trim().startsWith(XML_HEADER)) {
		if (fileHeader.contains(MZML_HEADER))
		    return RawDataFileType.MZML;
		if (fileHeader.contains(MZDATA_HEADER))
		    return RawDataFileType.MZDATA;
		if (fileHeader.contains(MZXML_HEADER))
		    return RawDataFileType.MZXML;
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

	return null;

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.RAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return RawDataImportParameters.class;
    }

}
