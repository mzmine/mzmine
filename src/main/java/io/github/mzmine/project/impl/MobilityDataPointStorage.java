/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MobilityDataPointStorage {

  private static final int NUM_ENTRIES = 4;

  private static final Logger logger = Logger.getLogger(MobilityDataPointStorage.class.getName());
  private final TreeMap<Integer, Long> dataPointsOffsets;
  private final TreeMap<Integer, Integer> dataPointsLengths;
  private ByteBuffer buffer = ByteBuffer.allocate(20000);
//  private File dataPointsFileName;
  private RandomAccessFile dataPointsFile;

  public MobilityDataPointStorage() {
    dataPointsOffsets = new TreeMap<>();
    dataPointsLengths = new TreeMap<>();
  }

  /**
   * Create a new temporary data points file
   */
  private File createNewDataPointsFile() throws IOException {
    return File.createTempFile("mzmine", ".mobilograms");
  }

  /**
   * Opens the given file as a data points file for this RawDataFileImpl instance. If the file is
   * not empty, the TreeMaps supplied as parameters have to describe the mapping of storage IDs to
   * data points in the file.
   */
  public synchronized void openDataPointsFile(File dataPointsFileName) throws IOException {
    if (this.dataPointsFile != null) {
      throw new IOException("Cannot open another data points file, because one is already open");
    }
//    this.dataPointsFileName = dataPointsFileName;
    this.dataPointsFile = new RandomAccessFile(dataPointsFileName, "rw");

    // Locks the temporary file so it is not removed when another instance
    // of MZmine is starting. Lock will be automatically released when this
    // instance of MZmine exits. Locking may fail on network-mounted
    // filesystems.
    try {
      FileChannel fileChannel = dataPointsFile.getChannel();
      fileChannel.lock();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to lock the file " + dataPointsFileName, e);
    }

    // Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
    // bug #4171239. We will try to remove the temporary files in a
    // shutdown hook registered in the main.ShutDownHook class
    dataPointsFileName.deleteOnExit();
  }

  /**
   * Returns the (already opened) data points file. Warning: may return null in case no scans have
   * been added yet to this RawDataFileImpl instance
   */
  public RandomAccessFile getDataPointsFile() {
    return dataPointsFile;
  }

  public synchronized int storeDataPoints(List<MobilityDataPoint> dataPoints) throws IOException {

    if (dataPointsFile == null) {
      File newFile = createNewDataPointsFile();
      openDataPointsFile(newFile);
    }

    final long currentOffset = dataPointsFile.length();

    final int currentID;
    if (!dataPointsOffsets.isEmpty()) {
      currentID = dataPointsOffsets.lastKey() + 1;
    } else {
      currentID = 1;
    }

    final int numOfDataPoints = dataPoints.size();

    // Convert the dataPoints into a byte array. Each double takes 8 bytes,
    // so we get the current double offset by dividing the size of the file
    // by 8
    final int numOfBytes = numOfDataPoints * NUM_ENTRIES * 8;

    if (buffer.capacity() < numOfBytes) {
      buffer = ByteBuffer.allocate(numOfBytes * 2);
    } else {
      // JDK 9 breaks compatibility with JRE8: need to cast
      // https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime
      ((Buffer) buffer).clear();
    }

    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
    for (MobilityDataPoint dp : dataPoints) {
      doubleBuffer.put(dp.getMZ());
      doubleBuffer.put(dp.getIntensity());
      doubleBuffer.put(dp.getMobility());
      doubleBuffer.put((double)dp.getScanNum());
    }

    dataPointsFile.seek(currentOffset);
    dataPointsFile.write(buffer.array(), 0, numOfBytes);

    dataPointsOffsets.put(currentID, currentOffset);
    dataPointsLengths.put(currentID, numOfDataPoints);

    return currentID;

  }

  public synchronized List<MobilityDataPoint> readDataPoints(int ID) throws IOException {

    final Long currentOffset = dataPointsOffsets.get(ID);
    final Integer numOfDataPoints = dataPointsLengths.get(ID);

    if ((currentOffset == null) || (numOfDataPoints == null)) {
      throw new IllegalArgumentException("Unknown storage ID " + ID);
    }

    final int numOfBytes = numOfDataPoints * NUM_ENTRIES * 8;

    if (buffer.capacity() < numOfBytes) {
      buffer = ByteBuffer.allocate(numOfBytes * 2);
    } else {
      // JDK 9 breaks compatibility with JRE8: need to cast
      // https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime
      ((Buffer) buffer).clear();
    }

    dataPointsFile.seek(currentOffset);
    dataPointsFile.read(buffer.array(), 0, numOfBytes);

    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
    List<MobilityDataPoint> dataPoints = new ArrayList<>(numOfDataPoints);

    for (int i = 0; i < numOfDataPoints; i++) {
      double mz = doubleBuffer.get();
      double intensity = doubleBuffer.get();
      double mobility = doubleBuffer.get();
      int scanNum = (int) doubleBuffer.get();
      dataPoints.add(new MobilityDataPoint(mz, intensity, mobility, scanNum));
    }

    return dataPoints;
  }

  public synchronized void removeStoredDataPoints(int ID) throws IOException {
    dataPointsOffsets.remove(ID);
    dataPointsLengths.remove(ID);
  }
}
