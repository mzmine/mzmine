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

package net.sf.mzmine.modules.peakpicking.shapemodeler;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;

class ShapeModelerTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private int processedScans, totalScans;

    private RawDataFile dataFiles[];

    ShapeModelerTask(PeakListRow peakListRow, RawDataFile dataFiles[],
            ShapeModelerParameters parameters) {


    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0f;
        return (double) processedScans / totalScans;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Shape modeling peaks from " + Arrays.toString(dataFiles);
    }

    public void run() {

        status = TaskStatus.PROCESSING;

	/*	try {

			String peakModelClassName = null;

			for (int modelIndex = 0; modelIndex < SavitzkyGolayPeakDetectorParameters.peakModelNames.length; modelIndex++) {
				if (SavitzkyGolayPeakDetectorParameters.peakModelNames[modelIndex]
						.equals(peakModelname))
					peakModelClassName = SavitzkyGolayPeakDetectorParameters.peakModelClasses[modelIndex];
				;
			}

			if (peakModelClassName == null)
				throw new ClassNotFoundException();

			peakModelClass = Class.forName(peakModelClassName);

			//peakModel = (PeakModel) peakModelClass.newInstance();

		} catch (Exception e) {
			logger.warning("Error trying to make an instance of peak model "
					+ peakModelname);
		}
*/
        logger.finest("Finished xxx peak picker" + processedScans
                + " scans processed");

        status = TaskStatus.FINISHED;

    }

}
