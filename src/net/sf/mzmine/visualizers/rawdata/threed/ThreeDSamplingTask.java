/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.visualizers.rawdata.threed;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.util.TimeNumberFormat;
import net.sf.mzmine.util.MyMath.BinningType;
import visad.AxisScale;
import visad.CellImpl;
import visad.ConstantMap;
import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.GraphicsModeControl;
import visad.Gridded2DSet;
import visad.GriddedSet;
import visad.Linear2DSet;
import visad.MathType;
import visad.MouseHelper;
import visad.ProjectionControl;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.SI;
import visad.ScalarMap;
import visad.Set;
import visad.Text;
import visad.TextControl;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;
import visad.bom.PickManipulationRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.DisplayRendererJ3D;
import visad.java3d.KeyboardBehaviorJ3D;
import visad.java3d.MouseBehaviorJ3D;

/**
 *
 */
class ThreeDSamplingTask implements Task {

    private ThreeDVisualizer visualizer;
    private RawDataFile rawDataFile;
    private int scanNumbers[];
    private double rtMin, rtMax, mzMin, mzMax;

    // data resolution on m/z and retention time axis
    private int rtResolution, mzResolution;

    private int retrievedScans = 0;
    private TaskStatus status;
    private String errorMessage;

    // The 3D display
    private DisplayImplJ3D display;


    // maximum value on Z axis
    private float maxBinnedIntensity;

    // number of labeled ticks on retention time axis
    private static final int X_AXIS_TICKS = 30;

    // number of labeled ticks on m/z axis
    private static final int Y_AXIS_TICKS = 30;

    // number of labeled ticks (marks) on intensity axis
    private static final int Z_AXIS_TICKS = 10;

    // number of minor (not labeled) axis ticks per 1 major tick
    private static final int MINOR_TICKS = 5;

    // tick labels font size
    private static final int LABEL_FONT_SIZE = 2;


    // axes aspect ratio X:Y:Z
    private static final double[] ASPECT_RATIO = new double[] { 1, 0.8, 0.3 };


    // TODO: get these from parameter storage
    private static NumberFormat rtFormat = new TimeNumberFormat();
    private static NumberFormat mzFormat = new DecimalFormat("0.##");
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");


    /**
     * Task constructor
     * @param rawDataFile
     * @param msLevel
     * @param visualizer
     */
    ThreeDSamplingTask(RawDataFile rawDataFile, int scanNumbers[],
            double rtMin, double rtMax,
            double mzMin, double mzMax,
            int rtResolution, int mzResolution,
            ThreeDVisualizer visualizer) {

        status = TaskStatus.WAITING;

        this.rawDataFile = rawDataFile;
        this.scanNumbers = scanNumbers;

        this.rtMin = rtMin;
        this.rtMax = rtMax;
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.rtResolution = rtResolution;
        this.mzResolution = mzResolution;

        this.visualizer = visualizer;


    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Sampling 3D plot of " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return (float) retrievedScans / scanNumbers.length;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        return display;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        try {

            // get the data range
            final float mzRange = (float) (mzMax - mzMin);
            final float rtRange = (float) (rtMax - rtMin);


            // create 3D display
            display = new DisplayImplJ3D(rawDataFile.toString());

            // basic types
            // we have to use "RT", "Retention time" returns null(?)
            RealType retentionTimeType = RealType.getRealType("RT", SI.second);
            RealType mzType = RealType.getRealType("m/z");
            RealType intensityType = RealType.getRealType("Intensity");

            // annotation type
            TextType annotationType = TextType.getTextType("Annotation");

            // peak height is for picked peaks, same as intensity, but not mapped to color
            RealType peakHeightType = RealType.getRealType("Height");

            // function domain - R^2 (retention time and m/z)
            RealTupleType domainTuple = new RealTupleType(retentionTimeType, mzType);

            // [X:Y:Z] tuple for drawing peak box
            RealTupleType pointTupleType = new RealTupleType(retentionTimeType, mzType, peakHeightType);


            // annotation range
            TupleType annotationTupleType = new TupleType(new MathType[] { peakHeightType, annotationType });

            // domain values set
            Set domainSet;

            // set the resolution (number of data points) on m/z axis
            final float mzStep = mzRange / mzResolution;


            // set the resolution (number of data points) on retention time axis
            if (scanNumbers.length > rtResolution) {

                // if the number of scans exceeds MAXIMUM_SCANS, we have to bin scans

                domainSet = new Linear2DSet(domainTuple,
                        rtMin,
                        rtMax,
                        rtResolution,
                        mzMin,
                        mzMax,
                        mzResolution);

            } else {

                // number of scans is lower then max. resolution, so we can create a grid column for each scan
                rtResolution = scanNumbers.length;

                // domain points in 2D grid
                float domainPoints[][] = new float[2][mzResolution * rtResolution];

                for (int j = 0; j < mzResolution; j++) {
                    for (int i = 0; i < rtResolution; i++) {

                        // set the point's X coordinate
                        domainPoints[0][(rtResolution * j) + i] = (float) rawDataFile.getRetentionTime(scanNumbers[i]);

                        // set the point's Y coordinate
                        domainPoints[1][(rtResolution * j) + i] = (float) mzMin + (j * mzStep);
                    }
                }

                domainSet = new Gridded2DSet(domainTuple, domainPoints,
                        rtResolution, mzResolution);

            }

            final float rtStep = rtRange / rtResolution;

            // create an array for all data points
            float[][] intensityValues = new float[1][mzResolution * rtResolution];

            // load scans
            Scan scan;
            int scanBinIndex;
            for (int scanIndex = 0; scanIndex < scanNumbers.length; scanIndex++) {

                if (status == TaskStatus.CANCELED)
                    return;

                scan = rawDataFile.getScan(scanNumbers[scanIndex]);

                double[] binnedIntensities = MyMath.binValues(scan.getMZValues(),
                        scan.getIntensityValues(),
                        mzMin,
                        mzMax,
                        mzResolution,
                        false,
                        BinningType.MAX);


                if (domainSet instanceof Linear2DSet) {
                    double rt = scan.getRetentionTime();
                    scanBinIndex = (int) ((rt - rtMin) / rtStep);

                    // last scan falls into last bin
                    if (scanBinIndex == rtResolution) scanBinIndex--;

                } else {
                    // 1 scan per 1 grid column
                    scanBinIndex = scanIndex;
                }

                for (int mzIndex = 0; mzIndex < mzResolution; mzIndex++) {

                    int intensityValuesIndex = (rtResolution * mzIndex) + scanBinIndex;

                    if (binnedIntensities[mzIndex] > intensityValues[0][intensityValuesIndex])
                        intensityValues[0][intensityValuesIndex] = (float) binnedIntensities[mzIndex];

                    if (intensityValues[0][intensityValuesIndex] > maxBinnedIntensity)
                        maxBinnedIntensity = (float) binnedIntensities[mzIndex];
                }

                retrievedScans++;

            }


            // create a function from domain (retention time and m/z) to intensity
            FunctionType intensityFunction = new FunctionType(domainTuple, intensityType);

            // create a function from domain (retention time and m/z) to text annotation
            FunctionType annotationFunction = new FunctionType(domainTuple, annotationTupleType);

            // sampled Intensity values stored in 1D array (FlatField)
            FlatField intensityValuesFlatField = new FlatField(intensityFunction, domainSet);
            intensityValuesFlatField.setSamples(intensityValues, false);


            // create a DataReference connecting data to display
            DataReference dataReference = new DataReferenceImpl("data");
            dataReference.setData(intensityValuesFlatField);
            display.addReference(dataReference);


            // if we have peak data, connect them to the display, too
            PeakList peakList = MZmineProject.getCurrentProject().getPeakList(rawDataFile);
            if (peakList != null) {

                Peak peaks[] = peakList.getPeaksInsideScanAndMZRange(rtMin, rtMax, mzMin, mzMax);

                if (peaks.length > 0) {

                    float peaksDomainPoints[][] = new float[2][peaks.length];
                    Data peakValues[] = new Data[peaks.length];

                    for (int i = 0; i < peaks.length; i++) {

                        peaksDomainPoints[0][i] = (float) peaks[i].getRawRT();
                        peaksDomainPoints[1][i] = (float) peaks[i].getRawMZ();

                        Data[] peakData = new Data[2];
                        peakData[0] = new Real(peakHeightType,
                                peaks[i].getRawHeight()
                                        + (maxBinnedIntensity * 0.03));
                        peakData[1] = new Text(annotationType,
                                mzFormat.format(peaks[i].getRawMZ()));

                        peakValues[i] = new Tuple(annotationTupleType,
                                peakData, false);

                    }

                    // peak domain points set
                    Set peaksDomainSet = new Gridded2DSet(domainTuple,
                            peaksDomainPoints, peaks.length);

                    // create peak values flat field
                    FieldImpl peakValuesFlatField = new FieldImpl(
                            annotationFunction, peaksDomainSet);
                    peakValuesFlatField.setSamples(peakValues, false);

                    // create data reference
                    DataReference peaksReference = new DataReferenceImpl(
                            "peaks");
                    peaksReference.setData(peakValuesFlatField);

                    // create a pick renderer, so we can track user clicks
                    PickManipulationRendererJ3D pickRenderer = new PickManipulationRendererJ3D();

                    // color of text annotations
                    ConstantMap[] colorMap = {
                            new ConstantMap(0.8, Display.Red),
                            new ConstantMap(0.8, Display.Green),
                            new ConstantMap(0.0f, Display.Blue) };

                    // add the reference to the display
                    display.addReferences(pickRenderer, peaksReference,
                            colorMap);

                    visualizer.setPeaksDataReference(pickRenderer,
                            peaksReference, colorMap);

                    // add the reference to the cell - the cell is activated by
                    // shift+right mouse click
                    ThreeDPeakCell cell = new ThreeDPeakCell(display,
                            pickRenderer, peaks, pointTupleType, mzStep);
                    cell.addReference(peaksReference);

                }

            }


            // get graphics mode control to set axes and textures properties
            GraphicsModeControl dispGMC = display.getGraphicsModeControl();

            // show axis scales
            dispGMC.setScaleEnable(true);

            // no textures
            dispGMC.setTextureEnable(false);



            // create mapping for X,Y,Z axes and color
            ScalarMap retentionTimeMap = new ScalarMap(retentionTimeType, Display.XAxis);
            ScalarMap mzMap = new ScalarMap(mzType, Display.YAxis);
            ScalarMap intensityMap = new ScalarMap(intensityType, Display.ZAxis);
            ScalarMap heightMap = new ScalarMap(peakHeightType, Display.ZAxis);
            ScalarMap colorMap = new ScalarMap(intensityType, Display.RGB);
            ScalarMap annotationMap = new ScalarMap(annotationType, Display.Text);


            // add maps to display
            display.addMap(retentionTimeMap);
            display.addMap(mzMap);
            display.addMap(intensityMap);
            display.addMap(heightMap);
            display.addMap(colorMap);
            display.addMap(annotationMap);


            float ticks;

            // set retention time axis properties
            AxisScale retentionTimeAxis = retentionTimeMap.getAxisScale();
            retentionTimeAxis.setColor(Color.black);
            retentionTimeAxis.setTitle("Retention time");
            retentionTimeAxis.setLabelSize(LABEL_FONT_SIZE);
            retentionTimeAxis.setLabelAllTicks(true);
            ticks = Math.round(rtRange / X_AXIS_TICKS);
            retentionTimeAxis.setMinorTickSpacing(ticks / MINOR_TICKS);
            retentionTimeAxis.setMajorTickSpacing(ticks);
            retentionTimeAxis.setNumberFormat(rtFormat);

            // set m/z axis properties
            AxisScale mzAxis = mzMap.getAxisScale();
            mzAxis.setColor(Color.black);
            mzAxis.setLabelSize(LABEL_FONT_SIZE);
            mzAxis.setLabelAllTicks(true);
            mzAxis.setNumberFormat(mzFormat);
            ticks = Math.round(mzRange / Y_AXIS_TICKS);
            mzAxis.setMinorTickSpacing(ticks / MINOR_TICKS);
            mzAxis.setMajorTickSpacing(ticks);

            // set intensity axis properties
            AxisScale intensityAxis = intensityMap.getAxisScale();
            intensityAxis.setColor(Color.black);
            intensityAxis.setLabelSize(LABEL_FONT_SIZE);
            intensityAxis.setLabelAllTicks(true);
            intensityAxis.setNumberFormat(intensityFormat);
            ticks = Math.round(maxBinnedIntensity / Z_AXIS_TICKS);
            intensityAxis.setMinorTickSpacing(ticks / MINOR_TICKS);
            intensityAxis.setMajorTickSpacing(ticks);

            // height is the same as intensity
            AxisScale peakHeightAxis = heightMap.getAxisScale();
            peakHeightAxis.setVisible(false);

            // set ranges
            retentionTimeMap.setRange(rtMin, rtMax);
            mzMap.setRange(mzMin, mzMax);
            intensityMap.setRange(0, maxBinnedIntensity);
            heightMap.setRange(0, maxBinnedIntensity);


            // set the color axis top intensity to half of the maximum intensity value,
            // because the peaks are usually sharp
            colorMap.setRange(0, maxBinnedIntensity / 2);


            // get display renderer to set colors, box, mouse, keys..
            DisplayRendererJ3D dRenderer = (DisplayRendererJ3D) display.getDisplayRenderer();

            // set colors
            dRenderer.setForegroundColor(Color.black);
            dRenderer.setBackgroundColor(Color.white);

            // do not show box around the data
            dRenderer.setBoxOn(false);

            // set the mouse behavior
            int mouseBehavior[][][] = new int[][][] { { {
                MouseHelper.ROTATE,    // left mouse button
                MouseHelper.ZOOM       // SHIFT + left mouse button
                }, {
                MouseHelper.ROTATE,    // CTRL + left mouse button
                MouseHelper.ZOOM       // CTRL + SHIFT + left mouse button
                } }, { {
                MouseHelper.NONE,      // middle mouse button
                MouseHelper.NONE       // SHIFT + middle mouse button
                }, {
                MouseHelper.NONE,      // CTRL + middle mouse button
                MouseHelper.NONE       // CTRL + SHIFT + middle mouse button
                } }, { {
                MouseHelper.TRANSLATE, // right mouse button
                MouseHelper.DIRECT       // SHIFT + right mouse button
                }, {
                MouseHelper.TRANSLATE, // CTRL + right mouse button
                MouseHelper.DIRECT       // CTRL + SHIFT + right mouse button
                } } };
            dRenderer.getMouseBehavior().getMouseHelper().setFunctionMap(mouseBehavior);


            // set the keyboard behavior
            KeyboardBehaviorJ3D keyBehavior = new KeyboardBehaviorJ3D(dRenderer);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_X_POS, KeyEvent.VK_DOWN, 0);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_X_NEG, KeyEvent.VK_UP, 0);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Y_POS, KeyEvent.VK_LEFT, 0);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Y_NEG, KeyEvent.VK_RIGHT, 0);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Z_POS, KeyEvent.VK_PAGE_UP, 0);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Z_NEG, KeyEvent.VK_PAGE_DOWN, 0);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ZOOM_IN, KeyEvent.VK_PLUS, 0);
            keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ZOOM_OUT, KeyEvent.VK_MINUS, 0);
            dRenderer.addKeyboardBehavior(keyBehavior);


            // set text control properties
            TextControl textControl = (TextControl) annotationMap.getControl();
            textControl.setCenter(true);
            textControl.setAutoSize(false);
            textControl.setScale(0.1);


            // get projection control to set initial rotation and zooming
            ProjectionControl projCont = display.getProjectionControl();

            // set axes aspect ratio
            projCont.setAspect(ASPECT_RATIO);

            // get default projection matrix
            double[] pControlMatrix = projCont.getMatrix();

            // prepare rotation and scaling matrix
            double[] mult = MouseBehaviorJ3D.static_make_matrix(
                    75, 0, 0,   // rotation X,Y,Z
                    1,          // scaling
                    0.1, 0.2, 0 // translation (moving) X,Y,Z
                    );

            // multiply projection matrix
            pControlMatrix = MouseBehaviorJ3D.static_multiply_matrix(mult, pControlMatrix);

            // set new projection matrix
            projCont.setMatrix(pControlMatrix);


        } catch (Throwable e) {
            e.printStackTrace();
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        status = TaskStatus.FINISHED;

    }

}
