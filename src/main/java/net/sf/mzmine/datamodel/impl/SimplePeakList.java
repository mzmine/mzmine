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

package net.sf.mzmine.datamodel.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.impl.projecttree.PeakListTreeModel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.MZmineProjectImpl;

import com.google.common.collect.Range;

/**
 * Simple implementation of the PeakList interface.
 */
public class SimplePeakList implements PeakList {

    private String name;
    private RawDataFile[] dataFiles;
    private ArrayList<PeakListRow> peakListRows;
    private double maxDataPointIntensity = 0;
    private Vector<PeakListAppliedMethod> descriptionOfAppliedTasks;
    private String dateCreated;
    private Range<Double> mzRange, rtRange;

    public static DateFormat dateFormat = new SimpleDateFormat(
	    "yyyy/MM/dd HH:mm:ss");

    public SimplePeakList(String name, RawDataFile dataFile) {
	this(name, new RawDataFile[] { dataFile });
    }

    public SimplePeakList(String name, RawDataFile[] dataFiles) {
	if ((dataFiles == null) || (dataFiles.length == 0)) {
	    throw (new IllegalArgumentException(
		    "Cannot create a peak list with no data files"));
	}
	this.name = name;
	this.dataFiles = new RawDataFile[dataFiles.length];

	RawDataFile dataFile;
	for (int i = 0; i < dataFiles.length; i++) {
	    dataFile = dataFiles[i];
	    this.dataFiles[i] = dataFile;
	}
	peakListRows = new ArrayList<PeakListRow>();
	descriptionOfAppliedTasks = new Vector<PeakListAppliedMethod>();

	dateCreated = dateFormat.format(new Date());

    }

    @Override
    public String getName() {
	return name;
    }

    @Override
    public String toString() {
	return name;
    }

    /**
     * Returns number of raw data files participating in the alignment
     */
    public int getNumberOfRawDataFiles() {
	return dataFiles.length;
    }

    /**
     * Returns all raw data files participating in the alignment
     */
    public RawDataFile[] getRawDataFiles() {
	return dataFiles;
    }

    public RawDataFile getRawDataFile(int position) {
	return dataFiles[position];
    }

    /**
     * Returns number of rows in the alignment result
     */
    public int getNumberOfRows() {
	return peakListRows.size();
    }

    /**
     * Returns the peak of a given raw data file on a give row of the alignment
     * result
     * 
     * @param row
     *            Row of the alignment result
     * @param rawDataFile
     *            Raw data file where the peak is detected/estimated
     */
    public Feature getPeak(int row, RawDataFile rawDataFile) {
	return peakListRows.get(row).getPeak(rawDataFile);
    }

    /**
     * Returns all peaks for a raw data file
     */
    public Feature[] getPeaks(RawDataFile rawDataFile) {
	Vector<Feature> peakSet = new Vector<Feature>();
	for (int row = 0; row < getNumberOfRows(); row++) {
	    Feature p = peakListRows.get(row).getPeak(rawDataFile);
	    if (p != null)
		peakSet.add(p);
	}
	return peakSet.toArray(new Feature[0]);
    }

    /**
     * Returns all peaks on one row
     */
    public PeakListRow getRow(int row) {
	return peakListRows.get(row);
    }

    public PeakListRow[] getRows() {
	return peakListRows.toArray(new PeakListRow[0]);
    }

    public PeakListRow[] getRowsInsideMZRange(Range<Double> mzRange) {
	Range<Double> all = Range.all();
	return getRowsInsideScanAndMZRange(all, mzRange);
    }

    public PeakListRow[] getRowsInsideScanRange(Range<Double> rtRange) {
	Range<Double> all = Range.all();
	return getRowsInsideScanAndMZRange(rtRange, all);
    }

    public PeakListRow[] getRowsInsideScanAndMZRange(Range<Double> rtRange,
	    Range<Double> mzRange) {
	Vector<PeakListRow> rowsInside = new Vector<PeakListRow>();

	for (PeakListRow row : peakListRows) {
	    if (rtRange.contains(row.getAverageRT())
		    && mzRange.contains(row.getAverageMZ()))
		rowsInside.add(row);
	}

	return rowsInside.toArray(new PeakListRow[0]);
    }

    public void addRow(PeakListRow row) {
	List<RawDataFile> myFiles = Arrays.asList(this.getRawDataFiles());
	for (RawDataFile testFile : row.getRawDataFiles()) {
	    if (!myFiles.contains(testFile))
		throw (new IllegalArgumentException("Data file " + testFile
			+ " is not in this peak list"));
	}

	peakListRows.add(row);
	if (row.getDataPointMaxIntensity() > maxDataPointIntensity) {
	    maxDataPointIntensity = row.getDataPointMaxIntensity();
	}

	if (mzRange == null) {
	    mzRange = Range.singleton(row.getAverageMZ());
	    rtRange = Range.singleton(row.getAverageRT());
	} else {
	    mzRange = mzRange.span(Range.singleton(row.getAverageMZ()));
	    rtRange = rtRange.span(Range.singleton(row.getAverageRT()));
	}
    }

    /**
     * Returns all peaks overlapping with a retention time range
     * 
     * @param startRT
     *            Start of the retention time range
     * @param endRT
     *            End of the retention time range
     * @return
     */
    public Feature[] getPeaksInsideScanRange(RawDataFile file,
	    Range<Double> rtRange) {
	Range<Double> all = Range.all();
	return getPeaksInsideScanAndMZRange(file, rtRange, all);
    }

    /**
     * @see net.sf.mzmine.datamodel.PeakList#getPeaksInsideMZRange(double,
     *      double)
     */
    public Feature[] getPeaksInsideMZRange(RawDataFile file,
	    Range<Double> mzRange) {
	Range<Double> all = Range.all();
	return getPeaksInsideScanAndMZRange(file, all, mzRange);
    }

    /**
     * @see net.sf.mzmine.datamodel.PeakList#getPeaksInsideScanAndMZRange(double,
     *      double, double, double)
     */
    public Feature[] getPeaksInsideScanAndMZRange(RawDataFile file,
	    Range<Double> rtRange, Range<Double> mzRange) {
	Vector<Feature> peaksInside = new Vector<Feature>();

	Feature[] peaks = getPeaks(file);
	for (Feature p : peaks) {
	    if (rtRange.contains(p.getRT()) && mzRange.contains(p.getMZ()))
		peaksInside.add(p);
	}

	return peaksInside.toArray(new Feature[0]);
    }

    /**
     * @see net.sf.mzmine.datamodel.PeakList#removeRow(net.sf.mzmine.datamodel.PeakListRow)
     */
    public void removeRow(PeakListRow row) {
	peakListRows.remove(row);

	// We have to update the project tree model
	MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore
		.getProjectManager().getCurrentProject();
	PeakListTreeModel treeModel = project.getPeakListTreeModel();
	treeModel.removeObject(row);

	updateMaxIntensity();
    }

    /**
     * @see net.sf.mzmine.datamodel.PeakList#removeRow(net.sf.mzmine.datamodel.PeakListRow)
     */
    public void removeRow(int rowNum) {
	removeRow(peakListRows.get(rowNum));
    }

    private void updateMaxIntensity() {
	maxDataPointIntensity = 0;
	mzRange = null;
	rtRange = null;
	for (PeakListRow peakListRow : peakListRows) {
	    if (peakListRow.getDataPointMaxIntensity() > maxDataPointIntensity)
		maxDataPointIntensity = peakListRow.getDataPointMaxIntensity();

	    if (mzRange == null) {
		mzRange = Range.singleton(peakListRow.getAverageMZ());
		rtRange = Range.singleton(peakListRow.getAverageRT());
	    } else {
		mzRange = mzRange.span(Range.singleton(peakListRow
			.getAverageMZ()));
		rtRange = rtRange.span(Range.singleton(peakListRow
			.getAverageRT()));
	    }
	}
    }

    /**
     * @see net.sf.mzmine.datamodel.PeakList#getPeakRowNum(net.sf.mzmine.datamodel.Feature)
     */
    public int getPeakRowNum(Feature peak) {

	PeakListRow rows[] = getRows();

	for (int i = 0; i < rows.length; i++) {
	    if (rows[i].hasPeak(peak))
		return i;
	}

	return -1;
    }

    /**
     * @see net.sf.mzmine.datamodel.PeakList#getDataPointMaxIntensity()
     */
    public double getDataPointMaxIntensity() {
	return maxDataPointIntensity;
    }

    public boolean hasRawDataFile(RawDataFile hasFile) {
	return Arrays.asList(dataFiles).contains(hasFile);
    }

    public PeakListRow getPeakRow(Feature peak) {
	PeakListRow rows[] = getRows();

	for (int i = 0; i < rows.length; i++) {
	    if (rows[i].hasPeak(peak))
		return rows[i];
	}

	return null;
    }

    public void setName(String name) {
	this.name = name;
    }

    public void addDescriptionOfAppliedTask(PeakListAppliedMethod appliedMethod) {
	descriptionOfAppliedTasks.add(appliedMethod);
    }

    public PeakListAppliedMethod[] getAppliedMethods() {
	return descriptionOfAppliedTasks.toArray(new PeakListAppliedMethod[0]);
    }

    public String getDateCreated() {
	return dateCreated;
    }

    public void setDateCreated(String date) {
	this.dateCreated = date;
    }

    public Range<Double> getRowsMZRange() {
	updateMaxIntensity(); // Update range before returning value
	return mzRange;
    }

    public Range<Double> getRowsRTRange() {
	updateMaxIntensity(); // Update range before returning value
	return rtRange;
    }

}
