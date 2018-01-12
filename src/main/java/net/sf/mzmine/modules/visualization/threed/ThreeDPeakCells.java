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

package net.sf.mzmine.modules.visualization.threed;

import java.rmi.RemoteException;

import net.sf.mzmine.datamodel.Feature;
import visad.CellImpl;
import visad.ConstantMap;
import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayImpl;
import visad.GriddedSet;
import visad.MathType;
import visad.VisADException;
import visad.bom.PickManipulationRendererJ3D;

import com.google.common.collect.Range;

/**
 * This class represents a 3D boxes which are displayed when user shift+clicks
 * on a peak in the 3D view
 */
public class ThreeDPeakCells extends CellImpl {

    private final DisplayImpl display;
    private Feature[] peaks;
    private DataReference[] references;
    private final MathType pointTupleType;
    private final PickManipulationRendererJ3D picker;

    /**
     * Create the peak cells.
     *
     * @param display3D
     *            the parent display.
     * @param pointType
     *            the point tuple type.
     * @param pickRenderer
     *            pick renderer.
     */
    public ThreeDPeakCells(final DisplayImpl display3D,
	    final MathType pointType,
	    final PickManipulationRendererJ3D pickRenderer) {

	// Initialize.
	display = display3D;
	pointTupleType = pointType;
	picker = pickRenderer;
	peaks = null;
	references = null;
    }

    /**
     * Set the peaks.
     *
     * @param thePeaks
     *            the peaks.
     * @throws VisADException
     *             if there are VisAD problems.
     * @throws RemoteException
     *             if there are VisAD problems.
     */
    public void setPeaks(final Feature[] thePeaks) throws VisADException,
	    RemoteException {
	synchronized (display) {

	    peaks = thePeaks.clone();

	    // First remove all existing data references.
	    if (references != null) {
		for (final DataReference reference : references) {
		    if (reference != null) {
			display.removeReference(reference);
		    }
		}
	    }

	    // Create new empty array for new data references
	    references = new DataReference[peaks.length];
	}
    }

    @Override
    public void doAction() throws VisADException, RemoteException {
	synchronized (display) {

	    final int index = picker.getCloseIndex();

	    if (index >= 0) {

		// Is the peak box already displayed?
		if (references[index] == null) {

		    // Create the peak box reference.
		    final DataReferenceImpl reference = new DataReferenceImpl(
			    "PeakShape");
		    reference.setData(createPeakBox(peaks[index]));

		    // Add the reference to the display
		    display.addReference(reference, new ConstantMap[] {
			    new ConstantMap(0.8, Display.Red),
			    new ConstantMap(0.2, Display.Green),
			    new ConstantMap(0.2, Display.Blue),
			    new ConstantMap(0.25, Display.Alpha) });

		    // Save the reference.
		    references[index] = reference;

		} else {

		    display.removeReference(references[index]);
		    references[index] = null;
		}
	    }
	}
    }

    /**
     * Create a peak bounding box.
     *
     * @param peak
     *            the peak.
     * @return the bounding box as a gridded set.
     * @throws VisADException
     *             if there are VisAD problems.
     */
    private Data createPeakBox(final Feature peak) throws VisADException {

	// Get the extents.
	final Range<Double> rtRange = peak.getRawDataPointsRTRange();
	final Range<Double> mzRange = peak.getRawDataPointsMZRange();
	final float rtMin = rtRange.lowerEndpoint().floatValue();
	final float rtMax = rtRange.upperEndpoint().floatValue();
	final float mzMin = mzRange.lowerEndpoint().floatValue();
	final float mzMax = mzRange.upperEndpoint().floatValue();
	final float heightMin = 1.0f;
	final float heightMax = peak.getRawDataPointsIntensityRange()
		.upperEndpoint().floatValue();

	// Create the box set.
	return GriddedSet.create(pointTupleType,
		new float[][] {
			{ rtMin, rtMax, rtMin, rtMax, rtMin, rtMax, rtMin,
				rtMax, rtMin, rtMin, rtMin, rtMin, rtMax,
				rtMax, rtMax, rtMax, },
			{ mzMin, mzMin, mzMin, mzMin, mzMax, mzMax, mzMax,
				mzMax, mzMin, mzMax, mzMin, mzMax, mzMin,
				mzMax, mzMin, mzMax },
			{ heightMin, heightMin, heightMax, heightMax,
				heightMax, heightMax, heightMin, heightMin,
				heightMin, heightMin, heightMax, heightMax,
				heightMax, heightMax, heightMin, heightMin } },
		new int[] { 2, 8 });
    }
}
