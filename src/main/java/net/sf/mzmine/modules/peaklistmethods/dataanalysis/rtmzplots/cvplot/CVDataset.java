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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.cvplot;

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

public class CVDataset extends AbstractXYZDataset implements RTMZDataset {

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

    public CVDataset(PeakList alignedPeakList, ParameterSet parameters) {

	int numOfRows = alignedPeakList.getNumberOfRows();

	RawDataFile selectedFiles[] = parameters.getParameter(
		CVParameters.dataFiles).getValue();
	PeakMeasurementType measurementType = parameters.getParameter(
		CVParameters.measurementType).getValue();

	// Generate title for the dataset
	datasetTitle = "Correlation of variation analysis";
	datasetTitle = datasetTitle.concat(" (");
	if (measurementType == PeakMeasurementType.AREA)
	    datasetTitle = datasetTitle.concat("CV of peak areas");
	else
	    datasetTitle = datasetTitle.concat("CV of peak heights");
	datasetTitle = datasetTitle.concat(" in " + selectedFiles.length
		+ " files");
	datasetTitle = datasetTitle.concat(")");

	logger.finest("Computing: " + datasetTitle);

	// Loop through rows of aligned peak list
	Vector<Double> xCoordsV = new Vector<Double>();
	Vector<Double> yCoordsV = new Vector<Double>();
	Vector<Double> colorCoordsV = new Vector<Double>();
	Vector<PeakListRow> peakListRowsV = new Vector<PeakListRow>();

	for (int rowIndex = 0; rowIndex < numOfRows; rowIndex++) {

	    PeakListRow row = alignedPeakList.getRow(rowIndex);

	    // Collect available peak intensities for selected files
	    Vector<Double> peakIntensities = new Vector<Double>();
	    for (int fileIndex = 0; fileIndex < selectedFiles.length; fileIndex++) {
		Feature p = row.getPeak(selectedFiles[fileIndex]);
		if (p != null) {
		    if (measurementType == PeakMeasurementType.AREA)
			peakIntensities.add(p.getArea());
		    else
			peakIntensities.add(p.getHeight());
		}
	    }

	    // If there are at least two measurements available for this peak
	    // then calc CV and include this peak in the plot
	    if (peakIntensities.size() > 1) {
		double[] ints = Doubles.toArray(peakIntensities);
		Double cv = MathUtils.calcCV(ints);

		Double rt = row.getAverageRT();
		Double mz = row.getAverageMZ();

		xCoordsV.add(rt);
		yCoordsV.add(mz);
		colorCoordsV.add(cv);
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
