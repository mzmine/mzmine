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

package net.sf.mzmine.project.impl;

import java.io.Serializable;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.project.MZmineProject;

/**
 * This class stores information about the sizes of different parts of
 * MZmineProjectImpl. It is the first item serialized into a stored project.
 * During project loading, it is the first item deserialized and provides
 * information about remaining items.
 *
 */
class StoredProjectDescription implements Serializable{

    private int numOfDataFiles, numOfScanFiles, numOfScans, numOfPeakLists, numOfPeakListRows;
    private long numOfScanFileBytes;

    /**
     * Create a new description based on given project.
     */
    StoredProjectDescription(MZmineProject project) {

        for (RawDataFile dataFile : project.getDataFiles()) {
            numOfDataFiles++;
            numOfScans += dataFile.getNumOfScans();
            RawDataFileImpl rawDataFileImpl = (RawDataFileImpl) dataFile;
            if (rawDataFileImpl.getScanDataFileasFile() != null) {
                numOfScanFiles++;
                numOfScanFileBytes += rawDataFileImpl.getScanDataFileasFile().length();
            }
        }

        for (PeakList peakList : project.getPeakLists()) {
            numOfPeakLists++;
            numOfPeakListRows += peakList.getNumberOfRows();
        }
    }

    /**
     * Returns the number of RawDataFiles in the project
     */
    int getNumOfDataFiles() {
        return numOfDataFiles;
    }

    /**
     * Returns the number of temporary scan data files (used to save raw data
     * points) used in the project. That is, the number of RawDataFiles with
     * preload level other than PRELOAD_ALL_SCANS.
     */
    int getNumOfScanFiles() {
        return numOfScanFiles;
    }

    /**
     * Returns the total number of scans in the project (in all data files)
     */
    int getNumOfScans() {
        return numOfScans;
    }

    /**
     * Returns the number of peak lists in the projects
     */
    int getNumOfPeakLists() {
        return numOfPeakLists;
    }

    /**
     * Returns the total number of peak list rows in the project (in all peak
     * lists)
     */
    int getNumOfPeakListRows() {
        return numOfPeakListRows;
    }

    /**
     * Returns the total number of bytes of all temporary scan data point files
     */
    long getTotalNumOfScanFileBytes() {
        return numOfScanFileBytes;
    }

}
