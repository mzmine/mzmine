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

package net.sf.mzmine.project.impl;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;

/**
 * Implementation of the Scan interface which stores raw data points in a
 * temporary file, accessed by RawDataFileImpl.readFromFloatBufferFile()
 */
public class StorableMassList implements MassList {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Scan scan;
    private String name;
    private RawDataFileImpl rawDataFile;
    private int storageID;

    public StorableMassList(RawDataFileImpl rawDataFile, int storageID,
	    String name, Scan scan) {
	this.scan = scan;
	this.name = name;
	this.rawDataFile = rawDataFile;
	this.storageID = storageID;
    }

    @Override
    public @Nonnull String getName() {
	return name;
    }

    @Override
    public @Nonnull Scan getScan() {
	return scan;
    }

    public void setScan(Scan scan) {
	this.scan = scan;
    }

    @Override
    public @Nonnull DataPoint[] getDataPoints() {
	try {
	    DataPoint result[] = rawDataFile.readDataPoints(storageID);
	    return result;

	} catch (IOException e) {
	    logger.severe("Could not read data from temporary file "
		    + e.toString());
	    return new DataPoint[0];
	}
    }

    public void removeStoredData() {
	try {
	    rawDataFile.removeStoredDataPoints(storageID);
	} catch (IOException e) {
	    logger.severe("Could not modify temporary file " + e.toString());
	}
	storageID = -1;
    }

    public int getStorageID() {
	return storageID;
    }

    @Override
    public String toString() {
	return name;
    }

}
