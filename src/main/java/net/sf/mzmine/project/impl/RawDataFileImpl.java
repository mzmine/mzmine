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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

/**
 * RawDataFile implementation. It provides storage of data points for scans and
 * mass lists using the storeDataPoints() and readDataPoints() methods. The data
 * points are stored in a temporary file (dataPointsFile) and the structure of
 * the file is stored in two TreeMaps. The dataPointsOffsets maps storage ID to
 * the offset in the dataPointsFile. The dataPointsLength maps the storage ID to
 * the number of data points stored under this ID. When stored data points are
 * deleted using removeStoredDataPoints(), the dataPointsFile is not modified,
 * the storage ID is just deleted from the two TreeMaps. When the project is
 * saved, the contents of the dataPointsFile are consolidated - only data points
 * referenced by the TreeMaps are saved (see the RawDataFileSaveHandler class).
 */
public class RawDataFileImpl implements RawDataFile, RawDataFileWriter {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    // Name of this raw data file - may be changed by the user
    private String dataFileName;

    private final Hashtable<Integer, Range<Double>> dataMZRange, dataRTRange;
    private final Hashtable<Integer, Double> dataMaxBasePeakIntensity,
	    dataMaxTIC;
    private final Hashtable<Integer, int[]> scanNumbersCache;

    private ByteBuffer buffer = ByteBuffer.allocate(20000);
    private final TreeMap<Integer, Long> dataPointsOffsets;
    private final TreeMap<Integer, Integer> dataPointsLengths;

    // Temporary file for scan data storage
    private File dataPointsFileName;
    private RandomAccessFile dataPointsFile;

    /**
     * Scans
     */
    private final Hashtable<Integer, StorableScan> scans;

    public RawDataFileImpl(String dataFileName) throws IOException {

	this.dataFileName = dataFileName;

	// Prepare the hashtables for scan numbers and data limits.
	scanNumbersCache = new Hashtable<Integer, int[]>();
	dataMZRange = new Hashtable<Integer, Range<Double>>();
	dataRTRange = new Hashtable<Integer, Range<Double>>();
	dataMaxBasePeakIntensity = new Hashtable<Integer, Double>();
	dataMaxTIC = new Hashtable<Integer, Double>();
	scans = new Hashtable<Integer, StorableScan>();
	dataPointsOffsets = new TreeMap<Integer, Long>();
	dataPointsLengths = new TreeMap<Integer, Integer>();

    }

    @Override
    public RawDataFile clone() throws CloneNotSupportedException {
        return (RawDataFile) super.clone();
    }
    
    /**
     * Create a new temporary data points file
     */
    public static File createNewDataPointsFile() throws IOException {
	return File.createTempFile("mzmine", ".scans");
    }

    /**
     * Returns the (already opened) data points file. Warning: may return null
     * in case no scans have been added yet to this RawDataFileImpl instance
     */
    public RandomAccessFile getDataPointsFile() {
	return dataPointsFile;
    }

    /**
     * Opens the given file as a data points file for this RawDataFileImpl
     * instance. If the file is not empty, the TreeMaps supplied as parameters
     * have to describe the mapping of storage IDs to data points in the file.
     */
    public synchronized void openDataPointsFile(File dataPointsFileName)
	    throws IOException {

	if (this.dataPointsFile != null) {
	    throw new IOException(
		    "Cannot open another data points file, because one is already open");
	}

	this.dataPointsFileName = dataPointsFileName;
	this.dataPointsFile = new RandomAccessFile(dataPointsFileName, "rw");

	// Locks the temporary file so it is not removed when another instance
	// of MZmine is starting. Lock will be automatically released when this
	// instance of MZmine exits. Locking may fail on network-mounted filesystems.
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
     * @see net.sf.mzmine.datamodel.RawDataFile#getNumOfScans()
     */
    public int getNumOfScans() {
	return scans.size();
    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFile#getScan(int)
     */
    public @Nonnull Scan getScan(int scanNumber) {
	return scans.get(scanNumber);
    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFile#getScanNumbers(int)
     */
    public @Nonnull int[] getScanNumbers(int msLevel) {
	if (scanNumbersCache.containsKey(msLevel))
	    return scanNumbersCache.get(msLevel);
	Range<Double> all = Range.all();
	int scanNumbers[] = getScanNumbers(msLevel, all);
	scanNumbersCache.put(msLevel, scanNumbers);
	return scanNumbers;
    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFile#getScanNumbers(int, double,
     *      double)
     */
    public @Nonnull int[] getScanNumbers(int msLevel,
	    @Nonnull Range<Double> rtRange) {

	assert rtRange != null;

	ArrayList<Integer> eligibleScanNumbers = new ArrayList<Integer>();

	Enumeration<StorableScan> scansEnum = scans.elements();
	while (scansEnum.hasMoreElements()) {
	    Scan scan = scansEnum.nextElement();

	    if ((scan.getMSLevel() == msLevel)
		    && (rtRange.contains(scan.getRetentionTime())))
		eligibleScanNumbers.add(scan.getScanNumber());
	}

	int[] numbersArray = Ints.toArray(eligibleScanNumbers);
	Arrays.sort(numbersArray);

	return numbersArray;
    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFile#getScanNumbers()
     */
    public @Nonnull int[] getScanNumbers() {

	if (scanNumbersCache.containsKey(0))
	    return scanNumbersCache.get(0);

	Set<Integer> allScanNumbers = scans.keySet();
	int[] numbersArray = Ints.toArray(allScanNumbers);
	Arrays.sort(numbersArray);

	scanNumbersCache.put(0, numbersArray);

	return numbersArray;

    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFile#getMSLevels()
     */
    public @Nonnull int[] getMSLevels() {

	Set<Integer> msLevelsSet = new HashSet<Integer>();

	Enumeration<StorableScan> scansEnum = scans.elements();
	while (scansEnum.hasMoreElements()) {
	    Scan scan = scansEnum.nextElement();
	    msLevelsSet.add(scan.getMSLevel());
	}

	int[] msLevels = Ints.toArray(msLevelsSet);
	Arrays.sort(msLevels);
	return msLevels;

    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFile#getDataMaxBasePeakIntensity()
     */
    public double getDataMaxBasePeakIntensity(int msLevel) {

	// check if we have this value already cached
	Double maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
	if (maxBasePeak != null)
	    return maxBasePeak;

	// find the value
	Enumeration<StorableScan> scansEnum = scans.elements();
	while (scansEnum.hasMoreElements()) {
	    Scan scan = scansEnum.nextElement();

	    // ignore scans of other ms levels
	    if (scan.getMSLevel() != msLevel)
		continue;

	    DataPoint scanBasePeak = scan.getHighestDataPoint();
	    if (scanBasePeak == null)
		continue;

	    if ((maxBasePeak == null)
		    || (scanBasePeak.getIntensity() > maxBasePeak))
		maxBasePeak = scanBasePeak.getIntensity();

	}

	// return -1 if no scan at this MS level
	if (maxBasePeak == null)
	    maxBasePeak = -1d;

	// cache the value
	dataMaxBasePeakIntensity.put(msLevel, maxBasePeak);

	return maxBasePeak;

    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFile#getDataMaxTotalIonCurrent()
     */
    public double getDataMaxTotalIonCurrent(int msLevel) {

	// check if we have this value already cached
	Double maxTIC = dataMaxTIC.get(msLevel);
	if (maxTIC != null)
	    return maxTIC.doubleValue();

	// find the value
	Enumeration<StorableScan> scansEnum = scans.elements();
	while (scansEnum.hasMoreElements()) {
	    Scan scan = scansEnum.nextElement();

	    // ignore scans of other ms levels
	    if (scan.getMSLevel() != msLevel)
		continue;

	    if ((maxTIC == null) || (scan.getTIC() > maxTIC))
		maxTIC = scan.getTIC();

	}

	// return -1 if no scan at this MS level
	if (maxTIC == null)
	    maxTIC = -1d;

	// cache the value
	dataMaxTIC.put(msLevel, maxTIC);

	return maxTIC;

    }

    public synchronized int storeDataPoints(DataPoint dataPoints[])
	    throws IOException {

	if (dataPointsFile == null) {
	    File newFile = RawDataFileImpl.createNewDataPointsFile();
	    openDataPointsFile(newFile);
	}

	final long currentOffset = dataPointsFile.length();

	final int currentID;
	if (!dataPointsOffsets.isEmpty())
	    currentID = dataPointsOffsets.lastKey() + 1;
	else
	    currentID = 1;

	final int numOfDataPoints = dataPoints.length;

	// Convert the dataPoints into a byte array. Each float takes 4 bytes,
	// so we get the current float offset by dividing the size of the file
	// by 4
	final int numOfBytes = numOfDataPoints * 2 * 4;

	if (buffer.capacity() < numOfBytes) {
	    buffer = ByteBuffer.allocate(numOfBytes * 2);
	} else {
	    buffer.clear();
	}

	FloatBuffer floatBuffer = buffer.asFloatBuffer();
	for (DataPoint dp : dataPoints) {
	    floatBuffer.put((float) dp.getMZ());
	    floatBuffer.put((float) dp.getIntensity());
	}

	dataPointsFile.seek(currentOffset);
	dataPointsFile.write(buffer.array(), 0, numOfBytes);

	dataPointsOffsets.put(currentID, currentOffset);
	dataPointsLengths.put(currentID, numOfDataPoints);

	return currentID;

    }

    public synchronized DataPoint[] readDataPoints(int ID) throws IOException {

	final Long currentOffset = dataPointsOffsets.get(ID);
	final Integer numOfDataPoints = dataPointsLengths.get(ID);

	if ((currentOffset == null) || (numOfDataPoints == null)) {
	    throw new IllegalArgumentException("Unknown storage ID " + ID);
	}

	final int numOfBytes = numOfDataPoints * 2 * 4;

	if (buffer.capacity() < numOfBytes) {
	    buffer = ByteBuffer.allocate(numOfBytes * 2);
	} else {
	    buffer.clear();
	}

	dataPointsFile.seek(currentOffset);
	dataPointsFile.read(buffer.array(), 0, numOfBytes);

	FloatBuffer floatBuffer = buffer.asFloatBuffer();

	DataPoint dataPoints[] = new DataPoint[numOfDataPoints];

	for (int i = 0; i < numOfDataPoints; i++) {
	    float mz = floatBuffer.get();
	    float intensity = floatBuffer.get();
	    dataPoints[i] = new SimpleDataPoint(mz, intensity);
	}

	return dataPoints;

    }

    public synchronized void removeStoredDataPoints(int ID) throws IOException {
	dataPointsOffsets.remove(ID);
	dataPointsLengths.remove(ID);
    }

    public synchronized void addScan(Scan newScan) throws IOException {

	// When we are loading the project, scan data file is already prepare
	// and we just need store the reference
	if (newScan instanceof StorableScan) {
	    scans.put(newScan.getScanNumber(), (StorableScan) newScan);
	    return;
	}

	DataPoint dataPoints[] = newScan.getDataPoints();
	final int storageID = storeDataPoints(dataPoints);

	StorableScan storedScan = new StorableScan(newScan, this,
		dataPoints.length, storageID);

	scans.put(newScan.getScanNumber(), storedScan);

    }

    /**
     * @see net.sf.mzmine.datamodel.RawDataFileWriter#finishWriting()
     */
    public synchronized RawDataFile finishWriting() throws IOException {
	for (StorableScan scan : scans.values()) {
	    scan.updateValues();
	}
	logger.finest("Writing of scans to file " + dataPointsFileName
		+ " finished");
	return this;
    }

    public @Nonnull Range<Double> getDataMZRange() {
	return getDataMZRange(0);
    }

    public @Nonnull Range<Double> getDataMZRange(int msLevel) {

	// check if we have this value already cached
	Range<Double> mzRange = dataMZRange.get(msLevel);
	if (mzRange != null)
	    return mzRange;

	// find the value
	for (Scan scan : scans.values()) {

	    // ignore scans of other ms levels
	    if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
		continue;

	    if (mzRange == null)
		mzRange = scan.getDataPointMZRange();
	    else
		mzRange = mzRange.span(scan.getDataPointMZRange());

	}

	// cache the value, if we found any
	if (mzRange != null)
	    dataMZRange.put(msLevel, mzRange);
	else
	    mzRange = Range.singleton(0.0);

	return mzRange;

    }

    public @Nonnull Range<Double> getDataRTRange() {
	return getDataRTRange(0);
    }

    public @Nonnull Range<Double> getDataRTRange(int msLevel) {

	// check if we have this value already cached
	Range<Double> rtRange = dataRTRange.get(msLevel);
	if (rtRange != null)
	    return rtRange;

	// find the value
	for (Scan scan : scans.values()) {

	    // ignore scans of other ms levels
	    if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
		continue;

	    if (rtRange == null)
		rtRange = Range.singleton(scan.getRetentionTime());
	    else
		rtRange = rtRange
			.span(Range.singleton(scan.getRetentionTime()));

	}

	// cache the value
	if (rtRange != null)
	    dataRTRange.put(msLevel, rtRange);
	else
	    rtRange = Range.singleton(0.0);

	return rtRange;

    }

    public void setRTRange(int msLevel, Range<Double> rtRange) {
	dataRTRange.put(msLevel, rtRange);
    }

    public void setMZRange(int msLevel, Range<Double> mzRange) {
	dataMZRange.put(msLevel, mzRange);
    }

    public int getNumOfScans(int msLevel) {
	return getScanNumbers(msLevel).length;
    }

    public synchronized TreeMap<Integer, Long> getDataPointsOffsets() {
	return dataPointsOffsets;
    }

    public synchronized TreeMap<Integer, Integer> getDataPointsLengths() {
	return dataPointsLengths;
    }

    public synchronized void close() {
	try {
	    if(dataPointsFileName != null) {
    	    	dataPointsFile.close();
    	    	dataPointsFileName.delete();
	    }
	} catch (IOException e) {
	    logger.warning("Could not close file " + dataPointsFileName + ": "
		    + e.toString());
	}
    }

    public @Nonnull String getName() {
	return dataFileName;
    }

    public void setName(@Nonnull String name) {
	this.dataFileName = name;
    }

    public String toString() {
	return dataFileName;
    }

}
