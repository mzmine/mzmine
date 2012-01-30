/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.batchmode;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

import org.w3c.dom.Document;

/**
 * Batch mode module
 */
public class BatchModeModule implements MZmineProcessingModule {

    private static Logger logger = Logger.getLogger(BatchModeModule.class
	    .getName());

    private static final String MODULE_NAME = "Batch mode";
    private static final String MODULE_DESCRIPTION = "This module allows execution of multiple processing tasks in a batch.";

    @Override
    public String getName() {
	return MODULE_NAME;
    }

    @Override
    public String getDescription() {
	return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull ParameterSet parameters,
	    @Nonnull Collection<Task> tasks) {
	BatchTask newTask = new BatchTask(parameters);
	tasks.add(newTask);
	return ExitCode.OK;
    }

    @Override
    public MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.PROJECT;
    }

    public static void runBatch(File batchFile) {

	logger.info("Running batch from file " + batchFile);

	try {
	    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
		    .newDocumentBuilder();
	    Document parsedBatchXML = docBuilder.parse(batchFile);
	    BatchQueue newQueue = BatchQueue.loadFromXml(parsedBatchXML
		    .getDocumentElement());
	    ParameterSet parameters = new BatchModeParameters();
	    parameters.getParameter(BatchModeParameters.batchQueue).setValue(
		    newQueue);
	    Task batchTask = new BatchTask(parameters);
	    batchTask.run();
	} catch (Exception e) {
	    logger.log(Level.SEVERE, "Error while running batch", e);
	    e.printStackTrace();
	}

    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
	return BatchModeParameters.class;
    }

}