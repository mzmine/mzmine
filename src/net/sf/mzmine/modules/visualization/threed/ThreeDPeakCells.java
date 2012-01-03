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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.threed;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.util.Range;
import visad.*;
import visad.bom.PickManipulationRendererJ3D;
import visad.java3d.DisplayImplJ3D;

import java.rmi.RemoteException;

/**
 * This class represents a 3D boxes which are displayed when user shift+clicks
 * on a peak in the 3D view
 */
public class ThreeDPeakCells extends CellImpl {

    private final DisplayImplJ3D display;
    private ChromatographicPeak[] peaks;
    private DataReference[] references;
    private final MathType pointTupleType;
    private final PickManipulationRendererJ3D picker;

    /**
     * Create the peak cells.
     *
     * @param display3D    the parent display.
     * @param pointType    the point tuple type.
     * @param pickRenderer pick renderer.
     */
    public ThreeDPeakCells(final DisplayImplJ3D display3D,
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
     * @param thePeaks the peaks.
     * @throws VisADException  if there are VisAD problems.
     * @throws RemoteException if there are VisAD problems.
     */
    public void setPeaks(final ChromatographicPeak[] thePeaks) throws VisADException,
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
                    final DataReferenceImpl reference = new DataReferenceImpl("PeakShape");
                    reference.setData(createPeakBox(peaks[index]));

                    // Add the reference to the display
                    display.addReference(reference,
                                         new ConstantMap[]{new ConstantMap(0.8, Display.Red),
                                                           new ConstantMap(0.2, Display.Green),
                                                           new ConstantMap(0.2, Display.Blue),
                                                           new ConstantMap(0.25, Display.Alpha)});

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
     * @param peak the peak.
     * @return the bounding box as a gridded set.
     * @throws VisADException if there are VisAD problems.
     */
    private Data createPeakBox(final ChromatographicPeak peak) throws VisADException {

        // Get the extents.
        final Range rtRange = peak.getRawDataPointsRTRange();
        final Range mzRange = peak.getRawDataPointsMZRange();
        final float rtMin = (float) rtRange.getMin();
        final float rtMax = (float) rtRange.getMax();
        final float mzMin = (float) mzRange.getMin();
        final float mzMax = (float) mzRange.getMax();
        final float heightMin = 1.0F;
        final float heightMax = (float) peak.getRawDataPointsIntensityRange().getMax();

        // Create the box set.
        return GriddedSet.create(
                pointTupleType,
                new float[][]{
                        {rtMin, rtMax, rtMin, rtMax, rtMin, rtMax, rtMin, rtMax,
                         rtMin, rtMin, rtMin, rtMin, rtMax, rtMax, rtMax, rtMax,},
                        {mzMin, mzMin, mzMin, mzMin, mzMax, mzMax, mzMax, mzMax,
                         mzMin, mzMax, mzMin, mzMax, mzMin, mzMax, mzMin, mzMax},
                        {heightMin, heightMin, heightMax, heightMax, heightMax,
                         heightMax, heightMin, heightMin, heightMin, heightMin,
                         heightMax, heightMax, heightMax, heightMax, heightMin,
                         heightMin}},
                new int[]{2, 8});
    }
}
