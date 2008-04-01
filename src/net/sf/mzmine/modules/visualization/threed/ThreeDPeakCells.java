/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.threed;

import java.rmi.RemoteException;

import net.sf.mzmine.data.Peak;
import visad.CellImpl;
import visad.ConstantMap;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.GriddedSet;
import visad.VisADException;

/**
 * This class represents a 3D boxes which are displayed when user shift+clicks
 * on a peak in the 3D view
 */
class ThreeDPeakCells extends CellImpl {

    private ThreeDDisplay display;
    private Peak[] peaks;
    private DataReference dataReferences[];

    ThreeDPeakCells(ThreeDDisplay display) {
        this.display = display;
    }

    synchronized void setPeaks(Peak peaks[]) throws VisADException,
            RemoteException {

        this.peaks = peaks;

        // First remove all existing data references
        if (dataReferences != null) {
            for (DataReference dr : dataReferences) {
                if (dr != null)
                    display.removeReference(dr);
            }
        }

        // Create new empty array for new data references
        dataReferences = new DataReference[peaks.length];

    }

    /**
     * @see visad.CellImpl#doAction()
     */
    synchronized public void doAction() throws VisADException, RemoteException {

        final int index = display.getPickRenderer().getCloseIndex();

        if (index < 0)
            return;

        // If we already have peak box displayed, remove it
        if (dataReferences[index] != null) {
            display.removeReference(dataReferences[index]);
            dataReferences[index] = null;
            return;
        }

        // Peak box bounds
        final float rtMin = peaks[index].getRawDataPointsRTRange().getMin();
        final float rtMax = peaks[index].getRawDataPointsRTRange().getMax();
        final float mzMin = peaks[index].getRawDataPointsMZRange().getMin();
        final float mzMax = peaks[index].getRawDataPointsMZRange().getMax();
        final float heightMin = 0;
        final float heightMax = peaks[index].getHeight();

        // Create the 8 lines (8 x 2 points) that form the peak box
        float points[][] = new float[][] {
                { rtMin, rtMax, rtMin, rtMax, rtMin, rtMax, rtMin, rtMax,
                        rtMin, rtMin, rtMin, rtMin, rtMax, rtMax, rtMax, rtMax, },
                { mzMin, mzMin, mzMin, mzMin, mzMax, mzMax, mzMax, mzMax,
                        mzMin, mzMax, mzMin, mzMax, mzMin, mzMax, mzMin, mzMax },
                { heightMin, heightMin, heightMax, heightMax, heightMax,
                        heightMax, heightMin, heightMin, heightMin, heightMin,
                        heightMax, heightMax, heightMax, heightMax, heightMin,
                        heightMin } };

        // I don't really understand this myself...
        int manifoldDimension[] = new int[] { 2, 8 };

        // Create a set of data points
        GriddedSet set = GriddedSet.create(display.getPointTupleType(), points,
                manifoldDimension);

        // Save the reference
        dataReferences[index] = new DataReferenceImpl("peakshape");
        dataReferences[index].setData(set);

        // Color and transparency of peak box
        ConstantMap[] colorMap = { new ConstantMap(1f, Display.Red),
                new ConstantMap(1.0f, Display.Green),
                new ConstantMap(0f, Display.Blue),
                new ConstantMap(0.25f, Display.Alpha) };

        // Add the reference to the display
        display.addReference(dataReferences[index], colorMap);

    }

}
