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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.logratioplot;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.RTMZDataset;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakMeasurementType;

import org.jfree.data.xy.AbstractXYZDataset;

import com.google.common.primitives.Doubles;

public class LogratioDataset extends AbstractXYZDataset implements RTMZDataset {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private double[] xCoords = new double[0];
    private double[] yCoords = new double[0];
    private double[] colorCoords = new double[0];
    private PeakListRow[] peakListRows = new PeakListRow[0];

    private String datasetTitle;

    public LogratioDataset(PeakList alignedPeakList, ParameterSet parameters) {
	int numOfRows = alignedPeakList.getNumberOfRows();

	RawDataFile groupOneFiles[] = parameters.getParameter(
		LogratioParameters.groupOneFiles).getValue();
	RawDataFile groupTwoFiles[] = parameters.getParameter(
		LogratioParameters.groupTwoFiles).getValue();
	PeakMeasurementType measurementType = parameters.getParameter(
		LogratioParameters.measurementType).getValue();

	// Generate title for the dataset
	datasetTitle = "Logratio analysis";
	datasetTitle = datasetTitle.concat(" (");
	if (measurementType == PeakMeasurementType.AREA)
	    datasetTitle = datasetTitle
		    .concat("Logratio of average peak areas");
	else
	    datasetTitle = datasetTitle
		    .concat("Logratio of average peak heights");
	datasetTitle = datasetTitle.concat(" in " + groupOneFiles.length
		+ " vs. " + groupTwoFiles.length + " files");
	datasetTitle = datasetTitle.concat(")");
	logger.finest("Computing: " + datasetTitle);

	Vector<Double> xCoordsV = new Vector<Double>();
	Vector<Double> yCoordsV = new Vector<Double>();
	Vector<Double> colorCoordsV = new Vector<Double>();
	Vector<PeakListRow> peakListRowsV = new Vector<PeakListRow>();

	for (int rowIndex = 0; rowIndex < numOfRows; rowIndex++) {

	    PeakListRow row = alignedPeakList.getRow(rowIndex);

	    // Collect available peak intensities for selected files
	    Vector<Double> groupOnePeakIntensities = new Vector<Double>();
	    for (int fileIndex = 0; fileIndex < groupOneFiles.length; fileIndex++) {
		Feature p = row.getPeak(groupOneFiles[fileIndex]);
		if (p != null) {
		    if (measurementType == PeakMeasurementType.AREA)
			groupOnePeakIntensities.add(p.getArea());
		    else
			groupOnePeakIntensities.add(p.getHeight());
		}
	    }
	    Vector<Double> groupTwoPeakIntensities = new Vector<Double>();
	    for (int fileIndex = 0; fileIndex < groupTwoFiles.length; fileIndex++) {
		Feature p = row.getPeak(groupTwoFiles[fileIndex]);
		if (p != null) {
		    if (measurementType == PeakMeasurementType.AREA)
			groupTwoPeakIntensities.add(p.getArea());
		    else
			groupTwoPeakIntensities.add(p.getHeight());
		}
	    }

	    // If there are at least one measurement from each group for this
	    // peak then calc logratio and include this peak in the plot
	    if ((groupOnePeakIntensities.size() > 0)
		    && (groupTwoPeakIntensities.size() > 0)) {

		double[] groupOneInts = Doubles
			.toArray(groupOnePeakIntensities);
		double groupOneAvg = MathUtils.calcAvg(groupOneInts);
		double[] groupTwoInts = Doubles
			.toArray(groupTwoPeakIntensities);
		double groupTwoAvg = MathUtils.calcAvg(groupTwoInts);
		double logratio = Double.NaN;
		if (groupTwoAvg != 0.0)
		    logratio = (double) (Math.log(groupOneAvg / groupTwoAvg) / Math
			    .log(2.0));

		Double rt = row.getAverageRT();
		Double mz = row.getAverageMZ();

		xCoordsV.add(rt);
		yCoordsV.add(mz);
		colorCoordsV.add(logratio);
		peakListRowsV.add(row);

	    }

	}

	// Finally store all collected values in arrays
	xCoords = Doubles.toArray(xCoordsV);
	yCoords = Doubles.toArray(yCoordsV);
	colorCoords = Doubles.toArray(colorCoordsV);
	peakListRows = peakListRowsV.toArray(new PeakListRow[0]);

    }

    public String toString() {
	return datasetTitle;
    }

    @Override
    public int getSeriesCount() {
	return 1;
    }

    @Override
    public Comparable<?> getSeriesKey(int series) {
	if (series == 0)
	    return new Integer(1);
	else
	    return null;
    }

    public Number getZ(int series, int item) {
	if (series != 0)
	    return null;
	if ((colorCoords.length - 1) < item)
	    return null;
	return colorCoords[item];
    }

    public int getItemCount(int series) {
	return xCoords.length;
    }

    public Number getX(int series, int item) {
	if (series != 0)
	    return null;
	if ((xCoords.length - 1) < item)
	    return null;
	return xCoords[item];
    }

    public Number getY(int series, int item) {
	if (series != 0)
	    return null;
	if ((yCoords.length - 1) < item)
	    return null;
	return yCoords[item];
    }

    public PeakListRow getPeakListRow(int item) {
	return peakListRows[item];
    }

}
