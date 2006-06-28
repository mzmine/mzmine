/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.threed;

import java.rmi.RemoteException;

import net.sf.mzmine.interfaces.Peak;
import visad.CellImpl;
import visad.ConstantMap;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.GriddedSet;
import visad.RealTupleType;
import visad.VisADException;
import visad.bom.PickManipulationRendererJ3D;
import visad.java3d.DisplayImplJ3D;

/**
 * 
 */
class ThreeDPeakCell extends CellImpl {

    private DisplayImplJ3D display;
    private PickManipulationRendererJ3D pickRenderer;
    private Peak[] peaks;
    private RealTupleType pointTupleType;
    private float mzStep;

    private DataReference dataReferences[];

    /**
     * @param display
     * @param pickRenderer
     * @param peaks
     * @param pointTupleType
     * @param mzStep
     */
    ThreeDPeakCell(DisplayImplJ3D display,
            PickManipulationRendererJ3D pickRenderer, Peak[] peaks,
            RealTupleType pointTupleType, float mzStep) {
        this.display = display;
        this.pickRenderer = pickRenderer;
        this.peaks = peaks;
        this.pointTupleType = pointTupleType;
        this.mzStep = mzStep;
        dataReferences = new DataReference[peaks.length];
    }

    /**
     * @see visad.CellImpl#doAction()
     */
    public void doAction() throws VisADException, RemoteException {

        final int index = pickRenderer.getCloseIndex();

        if (index < 0) return;

        // if we already have peak box displayed, remove it
        if (dataReferences[index] != null) {
            display.removeReference(dataReferences[index]);
            dataReferences[index] = null;
            return;
        }

        final float rtMin = (float) peaks[index].getMinRT();
        final float rtMax = (float) peaks[index].getMaxRT();
        final float mzMin = (float) peaks[index].getMZ() - mzStep;
        final float mzMax = (float) peaks[index].getMZ() + mzStep;
        final float heightMin = 0;
        final float heightMax = (float) peaks[index].getRawHeight();

        // create the 8 lines (8 x 2 points) that form the peak box
        float points[][] = new float[][] {
                { rtMin, rtMax, rtMin, rtMax, rtMin, rtMax, rtMin, rtMax,
                        rtMin, rtMin, rtMin, rtMin, rtMax, rtMax, rtMax, rtMax, },

                { mzMin, mzMin, mzMin, mzMin, mzMax, mzMax, mzMax, mzMax,
                        mzMin, mzMax, mzMin, mzMax, mzMin, mzMax, mzMin, mzMax },

                { heightMin, heightMin, heightMax, heightMax, heightMax,
                        heightMax, heightMin, heightMin, heightMin, heightMin,
                        heightMax, heightMax, heightMax, heightMax, heightMin,
                        heightMin } };

        final int manifoldDimension[] = new int[] { 2, 8 };
        
        GriddedSet set = GriddedSet.create(pointTupleType, points,
                manifoldDimension);

        // save the reference
        dataReferences[index] = new DataReferenceImpl("peakshape");
        dataReferences[index].setData(set);

        // color and transparency of peak box
        ConstantMap[] colorMap = { new ConstantMap(1f, Display.Red),
                new ConstantMap(1.0f, Display.Green),
                new ConstantMap(0f, Display.Blue),
                new ConstantMap(0.25f, Display.Alpha) };

        // add the reference to the display
        display.addReference(dataReferences[index], colorMap);

    }
}
