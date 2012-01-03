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
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;

/**
 * RawDataFile implementation
 */
public class RawDataFileImpl implements RawDataFile, RawDataFileWriter {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	// Name of this raw data file - may be changed by the user
	private String dataFileName;

	private Hashtable<Integer, Range> dataMZRange, dataRTRange;
	private Hashtable<Integer, Double> dataMaxBasePeakIntensity, dataMaxTIC;
	private Hashtable<Integer, int[]> scanNumbersCache;

	// Temporary file for scan data storage
	private File scanFile;
	private RandomAccessFile scanDataFile;

	/**
	 * Scans
	 */
	private Hashtable<Integer, Scan> scans;

	public RawDataFileImpl(String dataFileName) throws IOException {

		this.dataFileName = dataFileName;

		// Prepare the hashtables for scan numbers and data limits.
		scanNumbersCache = new Hashtable<Integer, int[]>();
		dataMZRange = new Hashtable<Integer, Range>();
		dataRTRange = new Hashtable<Integer, Range>();
		dataMaxBasePeakIntensity = new Hashtable<Integer, Double>();
		dataMaxTIC = new Hashtable<Integer, Double>();
		scans = new Hashtable<Integer, Scan>();

		scanFile = File.createTempFile("mzmine", ".scans");

		// Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
		// bug #4171239. We will try to remove the temporary files in a
		// shutdown hook registered in MZmineCore class
		scanFile.deleteOnExit();
		scanDataFile = new RandomAccessFile(scanFile, "rw");

		lockFile(scanDataFile);

	}

	public File getScanFile() {
		return scanFile;
	}

	public void openScanFile(File scanFile) throws IOException {

		this.scanFile = scanFile;
		this.scanDataFile = new RandomAccessFile(scanFile, "r");
		lockFile(scanDataFile);

	}

	/**
	 * Locks the temporary file so it is not removed when another instance of
	 * MZmine is starting. Lock will be automatically released when this
	 * instance of MZmine exits.
	 */
	private void lockFile(RandomAccessFile fileToLock) throws IOException {
		FileChannel scanFileChannel = fileToLock.getChannel();
		scanFileChannel.lock(0, fileToLock.length(), true);
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getNumOfScans()
	 */
	public int getNumOfScans() {
		return scans.size();
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScan(int)
	 */
	public Scan getScan(int scanNumber) {
		return scans.get(scanNumber);
	}

	/**
	 * Reads data from the temporary scan file
	 * 
	 * @param position
	 *            Position from the beginning of the file, in bytes
	 * @param size
	 *            Amount of data to read, in bytes
	 * @return
	 * @throws IOException
	 */
	synchronized ByteBuffer readFromFloatBufferFile(long position, int size)
			throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(size);

		scanDataFile.seek(position);
		scanDataFile.read(buffer.array(), 0, size);

		return buffer;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScanNumbers(int)
	 */
	public int[] getScanNumbers(int msLevel) {
		if (scanNumbersCache.containsKey(msLevel))
			return scanNumbersCache.get(msLevel);
		int scanNumbers[] = getScanNumbers(msLevel, new Range(
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
		scanNumbersCache.put(msLevel, scanNumbers);
		return scanNumbers;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScanNumbers(int, double, double)
	 */
	public int[] getScanNumbers(int msLevel, Range rtRange) {

		assert rtRange != null;

		ArrayList<Integer> eligibleScanNumbers = new ArrayList<Integer>();

		Enumeration<Scan> scansEnum = scans.elements();
		while (scansEnum.hasMoreElements()) {
			Scan scan = scansEnum.nextElement();

			if ((scan.getMSLevel() == msLevel)
					&& (rtRange.contains(scan.getRetentionTime())))
				eligibleScanNumbers.add(scan.getScanNumber());
		}

		int[] numbersArray = CollectionUtils.toIntArray(eligibleScanNumbers);
		Arrays.sort(numbersArray);

		return numbersArray;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScanNumbers()
	 */
	public int[] getScanNumbers() {

		if (scanNumbersCache.containsKey(0))
			return scanNumbersCache.get(0);

		Set<Integer> allScanNumbers = scans.keySet();
		int[] numbersArray = CollectionUtils.toIntArray(allScanNumbers);
		Arrays.sort(numbersArray);

		scanNumbersCache.put(0, numbersArray);

		return numbersArray;

	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getMSLevels()
	 */
	public int[] getMSLevels() {

		Set<Integer> msLevelsSet = new HashSet<Integer>();

		Enumeration<Scan> scansEnum = scans.elements();
		while (scansEnum.hasMoreElements()) {
			Scan scan = scansEnum.nextElement();
			msLevelsSet.add(scan.getMSLevel());
		}

		int[] msLevels = CollectionUtils.toIntArray(msLevelsSet);
		Arrays.sort(msLevels);
		return msLevels;

	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getDataMaxBasePeakIntensity()
	 */
	public double getDataMaxBasePeakIntensity(int msLevel) {

		// check if we have this value already cached
		Double maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
		if (maxBasePeak != null)
			return maxBasePeak;

		// find the value
		Enumeration<Scan> scansEnum = scans.elements();
		while (scansEnum.hasMoreElements()) {
			Scan scan = scansEnum.nextElement();

			// ignore scans of other ms levels
			if (scan.getMSLevel() != msLevel)
				continue;

			DataPoint scanBasePeak = scan.getBasePeak();
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
	 * @see net.sf.mzmine.data.RawDataFile#getDataMaxTotalIonCurrent()
	 */
	public double getDataMaxTotalIonCurrent(int msLevel) {

		// check if we have this value already cached
		Double maxTIC = dataMaxTIC.get(msLevel);
		if (maxTIC != null)
			return maxTIC.doubleValue();

		// find the value
		Enumeration<Scan> scansEnum = scans.elements();
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

	/**
     * 
     */
	public synchronized void addScan(Scan newScan) throws IOException {

		// When we are loading the project, scan data file is already prepare
		// and we just need store the references to StorableScans
		if (newScan instanceof StorableScan) {
			scans.put(newScan.getScanNumber(), newScan);
			return;
		}

		DataPoint dataPoints[] = newScan.getDataPoints();

		// Each float takes 4 bytes, so we get the current float offset by
		// dividing the size of the file by 4
		long currentOffset = scanDataFile.length();

		ByteBuffer buffer = ByteBuffer.allocate(dataPoints.length * 2 * 4);
		FloatBuffer floatBuffer = buffer.asFloatBuffer();

		for (DataPoint dp : dataPoints) {
			floatBuffer.put((float) dp.getMZ());
			floatBuffer.put((float) dp.getIntensity());
		}

		scanDataFile.write(buffer.array());

		StorableScan storedScan = new StorableScan(newScan, this,
				currentOffset, dataPoints.length);

		scans.put(newScan.getScanNumber(), storedScan);

	}

	/**
	 * @see net.sf.mzmine.data.RawDataFileWriter#finishWriting()
	 */
	public RawDataFile finishWriting() throws IOException {

		logger.finest("Writing of scans to file " + scanFile + " finished");

		// Close the temporary file and reopen it for read-only
		scanDataFile.close();
		openScanFile(scanFile);

		return this;

	}

	public Range getDataMZRange() {
		return getDataMZRange(0);
	}

	public Range getDataMZRange(int msLevel) {

		// check if we have this value already cached
		Range mzRange = dataMZRange.get(msLevel);
		if (mzRange != null)
			return mzRange;

		// find the value
		for (Scan scan : scans.values()) {

			// ignore scans of other ms levels
			if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
				continue;

			if (mzRange == null)
				mzRange = scan.getMZRange();
			else
				mzRange.extendRange(scan.getMZRange());

		}

		// cache the value, if we found any
		if (mzRange != null)
			dataMZRange.put(msLevel, mzRange);

		return mzRange;

	}

	public Range getDataRTRange() {
		return getDataRTRange(0);
	}

	public Range getDataRTRange(int msLevel) {

		// check if we have this value already cached
		Range rtRange = dataRTRange.get(msLevel);
		if (rtRange != null)
			return rtRange;

		// find the value
		for (Scan scan : scans.values()) {

			// ignore scans of other ms levels
			if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
				continue;

			if (rtRange == null)
				rtRange = new Range(scan.getRetentionTime());
			else
				rtRange.extendRange(scan.getRetentionTime());

		}

		// cache the value
		if (rtRange != null)
			dataRTRange.put(msLevel, rtRange);

		return rtRange;

	}

	public void setRTRange(int msLevel, Range rtRange) {
		dataRTRange.put(msLevel, rtRange);
	}

	public void setMZRange(int msLevel, Range mzRange) {
		dataMZRange.put(msLevel, mzRange);
	}

	public int getNumOfScans(int msLevel) {
		return getScanNumbers(msLevel).length;
	}

	public void close() {
		try {
			scanDataFile.close();
			scanFile.delete();
		} catch (IOException e) {
			logger.warning("Could not close file " + scanFile + ": "
					+ e.toString());
		}

		// Clear the scans array, to the garbage collector can collect it.
		// It may take time until this RawDataFileImpl instance is
		// garbage-collected, because there may be references to it from various
		// lists, parameter sets etc. But we can clean up the memory now,
		// because the data file is closed.
		scans = null;
		scanNumbersCache = null;
		dataMZRange = null;
		dataRTRange = null;
		dataMaxBasePeakIntensity = null;
		dataMaxTIC = null;

	}

	public String getName() {
		return dataFileName;
	}

	public void setName(String name) {
		this.dataFileName = name;
	}

	public String toString() {
		return dataFileName;
	}

}
