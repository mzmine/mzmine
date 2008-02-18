/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.qbixlipiddb;

import java.util.logging.Logger;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class QBIXLipidDBSearchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PeakList peakList;
	private QBIXLipidDBSearchParameters parameters;

	private TaskStatus status;
	private String errorMessage;
	private String[][] databaseValues;
	private int finishedLines = 0;

	QBIXLipidDBSearchTask(PeakList peakList,
			QBIXLipidDBSearchParameters parameters) {
		status = TaskStatus.WAITING;
		this.peakList = peakList;
		this.parameters = parameters;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public float getFinishedPercentage() {
		if (databaseValues == null)
			return 0;
		return ((float) finishedLines) / databaseValues.length;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Peak identification of " + peakList
				+ " using QBIX internal lipid database.";
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		logger.info("Running " + getTaskDescription());

		QBIXLipidDBQueriesBuilder queriesBuilder = new QBIXLipidDBQueriesBuilder(
				parameters);

		QBIXLipidDBConnection dbConnection = new QBIXLipidDBConnection(
				parameters);

		if (!dbConnection.openConnection()) {
			logger.severe("Could not open database connection");
			errorMessage = "Could not open database connection";
			status = TaskStatus.ERROR;
		}

		for (PeakListRow peakRow : peakList.getRows()) {

			QBIXLipidDBQuery[] queries = queriesBuilder.createQueries(peakRow);
			logger.finest("Created " + queries.length + " queries for row "
					+ peakRow.getAverageMZ() + ", " + peakRow.getAverageRT());

			for (QBIXLipidDBQuery query : queries) {
				CompoundIdentity[] foundIdentities = dbConnection
						.runQueryOnInternalDatabase(query);

				for (CompoundIdentity identity : foundIdentities)
					peakRow.addCompoundIdentity(identity);

				foundIdentities = dbConnection.runQueryOnLipidDatabase(query);

				for (CompoundIdentity identity : foundIdentities)
					peakRow.addCompoundIdentity(identity);
			}

			logger.finest("Added " + peakRow.getCompoundIdentities().length
					+ " identities for row " + peakRow.getAverageMZ() + ", "
					+ peakRow.getAverageRT());

		}

		dbConnection.closeConnection();

		logger.info("Finished " + getTaskDescription());

		status = TaskStatus.FINISHED;

	}

}
