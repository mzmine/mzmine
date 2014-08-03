/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.datamodel.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineObjectBuilder;

/**
 * This abstract class represents a storage mechanism for arrays of DataPoints.
 * Each RawDataFile and PeakList will use this mechanism to store their data
 * points on the disk, so these do not consume memory.
 * 
 * @see RawDataFileImpl
 * @see PeakListImpl
 */
abstract class DataPointStoreImpl {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final File tmpDataFileName;
    private final RandomAccessFile tmpDataFile;

    private ByteBuffer byteBuffer;
    private final TreeMap<Integer, Long> dataPointsOffsets;
    private final TreeMap<Integer, Integer> dataPointsLengths;
    private int lastStorageId = 0;

    DataPointStoreImpl() {

	try {
	    tmpDataFileName = File.createTempFile("mzmine", ".tmp");
	    tmpDataFile = new RandomAccessFile(tmpDataFileName, "rw");

	    /*
	     * Locks the temporary file so it is not removed when another
	     * instance of MZmine is starting. Lock will be automatically
	     * released when this instance of MZmine exits.
	     */
	    FileChannel fileChannel = tmpDataFile.getChannel();
	    fileChannel.lock();

	    /*
	     * Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
	     * bug #4171239. We will try to remove the temporary files in a
	     * shutdown hook registered in the main.ShutDownHook class.
	     */
	    tmpDataFileName.deleteOnExit();

	} catch (IOException e) {
	    throw new RuntimeException(
		    "I/O error while creating a new MZmine object", e);
	}

	byteBuffer = ByteBuffer.allocate(20000);
	dataPointsOffsets = new TreeMap<Integer, Long>();
	dataPointsLengths = new TreeMap<Integer, Integer>();

    }

    /**
     * Stores new array of data points.
     * 
     * @return Storage ID for the newly stored data.
     */
    synchronized int storeDataPoints(@Nonnull DataPoint dataPoints[])
	    throws IOException {

	final long currentOffset = tmpDataFile.length();

	final int numOfDataPoints = dataPoints.length;

	/*
	 * Convert the data points into a byte array. Each double takes 8 bytes,
	 * so we get the current float offset by dividing the size of the file
	 * by 8.
	 */
	final int numOfBytes = numOfDataPoints * 2 * 8;

	if (byteBuffer.capacity() < numOfBytes) {
	    byteBuffer = ByteBuffer.allocate(numOfBytes * 2);
	} else {
	    byteBuffer.clear();
	}

	DoubleBuffer dblBuffer = byteBuffer.asDoubleBuffer();
	for (DataPoint dp : dataPoints) {
	    dblBuffer.put(dp.getMZ());
	    dblBuffer.put(dp.getIntensity());
	}

	tmpDataFile.seek(currentOffset);
	tmpDataFile.write(byteBuffer.array(), 0, numOfBytes);

	// Increase the storage ID
	lastStorageId++;

	dataPointsOffsets.put(lastStorageId, currentOffset);
	dataPointsLengths.put(lastStorageId, numOfDataPoints);

	return lastStorageId;

    }

    /**
     * Reads the data points associated with given ID.
     */
    synchronized @Nonnull DataPoint[] readDataPoints(int ID) throws IOException {

	final Long currentOffset = dataPointsOffsets.get(ID);
	final Integer numOfDataPoints = dataPointsLengths.get(ID);

	if ((currentOffset == null) || (numOfDataPoints == null)) {
	    throw new IOException("Unknown storage ID " + ID);
	}

	final int numOfBytes = numOfDataPoints * 2 * 8;

	if (byteBuffer.capacity() < numOfBytes) {
	    byteBuffer = ByteBuffer.allocate(numOfBytes * 2);
	} else {
	    byteBuffer.clear();
	}

	tmpDataFile.seek(currentOffset);
	tmpDataFile.read(byteBuffer.array(), 0, numOfBytes);

	DoubleBuffer dblBuffer = byteBuffer.asDoubleBuffer();

	DataPoint dataPoints[] = new DataPoint[numOfDataPoints];
	double mz, intensity;

	for (int i = 0; i < numOfDataPoints; i++) {
	    mz = dblBuffer.get();
	    intensity = dblBuffer.get();
	    dataPoints[i] = MZmineObjectBuilder.getDataPoint(mz, intensity);
	}

	return dataPoints;

    }

    /**
     * Remove data associated with given storage ID. We do not attempt to remove
     * the data from disk, simply remove the reference to it.
     */
    synchronized void removeStoredDataPoints(long ID) {
	dataPointsOffsets.remove(ID);
	dataPointsLengths.remove(ID);
    }

    public synchronized void dispose() {
	if (!tmpDataFileName.exists())
	    return;
	try {
	    tmpDataFile.close();
	    tmpDataFileName.delete();
	} catch (IOException e) {
	    logger.warning("Could not close and remove temporary file "
		    + tmpDataFileName + ": " + e.toString());
	    e.printStackTrace();
	}
    }

    /**
     * When this object is garbage collected, remove the associated temporary
     * data file from disk.
     */
    @Override
    protected void finalize() {
	dispose();
    }
}
