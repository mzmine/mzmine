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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;

import org.w3c.dom.Document;

/**
 * Batch mode module
 */
public class BatchModeModule implements MZmineProcessingModule {

	private static Logger logger = Logger.getLogger(BatchModeModule.class
			.getName());

	private BatchModeParameters parameters = new BatchModeParameters();

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "Batch mode";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	@Override
	public Task[] runModule(ParameterSet parameters) {

		BatchTask newTask = new BatchTask(parameters);

		MZmineCore.getTaskController().addTask(newTask, TaskPriority.HIGH);

		return new Task[] { newTask };
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

}