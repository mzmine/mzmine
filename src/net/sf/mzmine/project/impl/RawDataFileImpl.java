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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.NameChangeable;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;

/**
 * RawDataFile implementation
 * 
 */
public class RawDataFileImpl implements RawDataFile, RawDataFileWriter,
		NameChangeable {

	private transient Logger logger = Logger.getLogger(this.getClass()
			.getName());

	private String fileName; // this is just a name of this object
	private String filePath;

	private Hashtable<Integer, Double> dataMinMZ, dataMaxMZ, dataMinRT,
			dataMaxRT, dataMaxBasePeakIntensity, dataMaxTIC;
	private Hashtable<Integer, int[]> scanNumbersCache;

	private transient File scanFile;
	private transient RandomAccessFile scanDataFile;

	/**
	 * Preloaded scans
	 */
	private Hashtable<Integer, Scan> scans;

	public RawDataFileImpl(String fileName) throws IOException {
		this.filePath = fileName;

		int separationLen = 0;
		if ((separationLen = this.filePath.lastIndexOf("/")) < 0) {
			separationLen = this.filePath.lastIndexOf("\\");
		}
		this.fileName = this.filePath.substring(separationLen + 1);

		// create temporary file for scan data
		File tmpFile = File.createTempFile("mzmine", ".scans");
		tmpFile.deleteOnExit();
		setScanDataFile(tmpFile);

		// prepare new Hashtable for scans
		scans = new Hashtable<Integer, Scan>();
		scanNumbersCache = new Hashtable<Integer, int[]>();

	}

	void setScanDataFile(File scanFile) throws IOException {
		this.scanFile = scanFile;

		scanDataFile = new RandomAccessFile(scanFile, "rw");

		// Lock the temporary file so it is not removed when another instance of
		// MZmine is starting. Lock will be automatically released when this
		// instance of MZmine exits.
		scanDataFile.getChannel().lock();
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getFilePath()
	 */
	public String getName() {
		return this.fileName;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getFilePath()
	 */
	public String getPath() {
		return this.filePath;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScanDataFile()
	 */
	RandomAccessFile getScanDataFile() {
		return scanDataFile;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScanDataFileasFile()
	 */
	public File getScanDataFileasFile() {
		return scanFile;
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
	 * @see net.sf.mzmine.data.RawDataFile#getDataMinMZ()
	 */
	public double getDataMinMZ(int msLevel) {

		// if there is no cache table, create one
		if (dataMinMZ == null)
			dataMinMZ = new Hashtable<Integer, Double>();

		// check if we have this value already cached
		Double minMZ = dataMinMZ.get(msLevel);
		if (minMZ != null)
			return minMZ.doubleValue();

		// find the value
		for (Scan scan : scans.values()) {

			// ignore scans of other ms levels
			if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
				continue;

			if ((minMZ == null) || (scan.getMZRange().getMin() < minMZ))
				minMZ = scan.getMZRange().getMin();
		}

		// return -1 if no scan at this MS level
		if (minMZ == null)
			minMZ = -1d;

		// cache the value
		dataMinMZ.put(msLevel, minMZ);

		return minMZ;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getDataMaxMZ()
	 */
	public double getDataMaxMZ(int msLevel) {

		// if there is no cache table, create one
		if (dataMaxMZ == null)
			dataMaxMZ = new Hashtable<Integer, Double>();

		// check if we have this value already cached
		Double maxMZ = dataMaxMZ.get(msLevel);
		if (maxMZ != null)
			return maxMZ.doubleValue();

		// find the value
		for (Scan scan : scans.values()) {

			// ignore scans of other ms levels
			if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
				continue;

			if ((maxMZ == null) || (scan.getMZRange().getMax() > maxMZ))
				maxMZ = scan.getMZRange().getMax();

		}

		// return -1 if no scan at this MS level
		if (maxMZ == null)
			maxMZ = -1d;

		// cache the value
		dataMaxMZ.put(msLevel, maxMZ);

		return maxMZ;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getDataMinRT()
	 */
	public double getDataMinRT(int msLevel) {

		Double minRT = null;

		// if there is no cache table, create one
		if (dataMinRT == null)
			dataMinRT = new Hashtable<Integer, Double>();
		else {
			minRT = dataMinRT.get(msLevel);
			if (minRT != null)
				return minRT.doubleValue();
		}

		// check if we have this value already cached
		// Double minRT = dataMinRT.get(msLevel);
		// if (minRT != null)
		// return minRT.doubleValue();

		// find the value
		Enumeration<Scan> scansEnum = scans.elements();
		while (scansEnum.hasMoreElements()) {
			Scan scan = scansEnum.nextElement();

			// ignore scans of other ms levels
			if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
				continue;

			if ((minRT == null) || (scan.getRetentionTime() < minRT))
				minRT = scan.getRetentionTime();

		}

		// return -1 if no scan at this MS level
		if (minRT == null)
			minRT = -1d;

		// cache the value
		dataMinRT.put(msLevel, minRT);

		return minRT;

	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getDataMaxRT()
	 */
	public double getDataMaxRT(int msLevel) {

		// if there is no cache table, create one
		if (dataMaxRT == null)
			dataMaxRT = new Hashtable<Integer, Double>();

		// check if we have this value already cached
		Double maxRT = dataMaxRT.get(msLevel);
		if (maxRT != null)
			return maxRT.doubleValue();

		// find the value
		Enumeration<Scan> scansEnum = scans.elements();
		while (scansEnum.hasMoreElements()) {
			Scan scan = scansEnum.nextElement();

			// ignore scans of other ms levels
			if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
				continue;

			if ((maxRT == null) || (scan.getRetentionTime() > maxRT))
				maxRT = scan.getRetentionTime();

		}

		// return -1 if no scan at this MS level
		if (maxRT == null)
			maxRT = -1d;

		// cache the value
		dataMaxRT.put(msLevel, maxRT);

		return maxRT;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScanNumbers(int)
	 */
	public int[] getScanNumbers(int msLevel) {
		if (scanNumbersCache.containsKey(msLevel))
			return scanNumbersCache.get(msLevel);
		int scanNumbers[] = getScanNumbers(msLevel, new Range(Double.MIN_VALUE,
				Double.MAX_VALUE));
		scanNumbersCache.put(msLevel, scanNumbers);
		return scanNumbers;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFile#getScanNumbers(int, double, double)
	 */
	public int[] getScanNumbers(int msLevel, Range rtRange) {

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

		Set<Integer> allScanNumbers = scans.keySet();
		int[] numbersArray = CollectionUtils.toIntArray(allScanNumbers);
		Arrays.sort(numbersArray);

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

		// if there is no cache table, create one
		if (dataMaxBasePeakIntensity == null)
			dataMaxBasePeakIntensity = new Hashtable<Integer, Double>();

		// check if we have this value already cached
		Double maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
		if (maxBasePeak != null)
			return maxBasePeak.doubleValue();

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

		// if there is no cache table, create one
		if (dataMaxTIC == null)
			dataMaxTIC = new Hashtable<Integer, Double>();

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
	public void addScan(Scan newScan) {

		int scanNumber = newScan.getScanNumber();

		// Store the scan data
		CachedStorableScan storedScan = new CachedStorableScan(newScan, this);
		scans.put(scanNumber, storedScan);

	}

	public String toString() {
		return fileName;
	}

	/**
	 * @see net.sf.mzmine.data.RawDataFileWriter#finishWriting()
	 */
	public RawDataFile finishWriting() throws IOException {

		logger.finest("Writing of scans to file " + scanFile + " finished");

		// switch temporary file to current datafile and reopen it for reading
		scanDataFile.close();

		scanDataFile = new RandomAccessFile(scanFile, "r");

		// Lock the temporary file so it is not removed when another instance of
		// MZmine is starting. Lock will be automatically released when this
		// instance of MZmine exits.
		scanDataFile.getChannel().lock(0L, Long.MAX_VALUE, true);

		return this;

	}

	public double getDataMaxMZ() {
		return getDataMaxMZ(0);
	}

	public double getDataMaxRT() {
		return getDataMaxRT(0);
	}

	public double getDataMinMZ() {
		return getDataMinMZ(0);
	}

	public double getDataMinRT() {
		return getDataMinRT(0);
	}

	public Range getDataMZRange() {
		// TODO this needs cleanup
		return new Range(getDataMinMZ(), getDataMaxMZ());
	}

	public Range getDataMZRange(int msLevel) {
		// TODO this needs cleanup
		return new Range(getDataMinMZ(msLevel), getDataMaxMZ(msLevel));
	}

	public Range getDataRTRange() {
		// TODO this needs cleanup
		return new Range(getDataMinRT(), getDataMaxRT());
	}

	public Range getDataRTRange(int msLevel) {
		// TODO this needs cleanup
		return new Range(getDataMinRT(msLevel), getDataMaxRT(msLevel));
	}

	public void setRTRange(int msLevel, Range rtRange) {

		// if there is no cache table, create one
		if (dataMinRT == null)
			dataMinRT = new Hashtable<Integer, Double>();

		// cache the value
		dataMinRT.put(msLevel, rtRange.getMin());

		// if there is no cache table, create one
		if (dataMaxRT == null)
			dataMaxRT = new Hashtable<Integer, Double>();

		// cache the value
		dataMaxRT.put(msLevel, rtRange.getMax());

	}

	public void setMZRange(int msLevel, Range mzRange) {

		// if there is no cache table, create one
		if (dataMinMZ == null)
			dataMinMZ = new Hashtable<Integer, Double>();

		// cache the value
		dataMinMZ.put(msLevel, mzRange.getMin());

		// if there is no cache table, create one
		if (dataMaxMZ == null)
			dataMaxMZ = new Hashtable<Integer, Double>();

		// cache the value
		dataMaxMZ.put(msLevel, mzRange.getMax());

	}

	public int getNumOfScans(int msLevel) {
		int numOfScans = 0;
		Enumeration<Scan> scansEnum = scans.elements();
		while (scansEnum.hasMoreElements()) {
			Scan scan = scansEnum.nextElement();
			if (scan.getMSLevel() == msLevel)
				numOfScans++;
		}
		return numOfScans;
	}

	public void close() {
		try {
			scanDataFile.close();
			scanFile.delete();
		} catch (IOException e) {
			logger.warning("Could not close file " + scanFile + ": "
					+ e.toString());
		}

	}

	public void setName(String name) {
		this.fileName = name;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object value) {
		RawDataFileImpl dataFile = (RawDataFileImpl) value;
		File comparedScanFile = dataFile.getScanDataFileasFile();
		if (comparedScanFile == null)
			return 1;
		return comparedScanFile.compareTo(scanFile);
	}

}
