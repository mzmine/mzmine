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
import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.util.TimeNumberFormat;
import net.sf.mzmine.util.MyMath.BinningType;
import visad.AxisScale;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.GraphicsModeControl;
import visad.Gridded2DSet;
import visad.Linear2DSet;
import visad.MouseHelper;
import visad.ProjectionControl;
import visad.RealTupleType;
import visad.RealType;
import visad.SI;
import visad.ScalarMap;
import visad.Set;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.DisplayRendererJ3D;
import visad.java3d.KeyboardBehaviorJ3D;
import visad.java3d.MouseBehaviorJ3D;

/**
 * 
 */
class ThreeDSamplingTask implements Task {

    private RawDataFile rawDataFile;
    private int scanNumbers[];
    private int msLevel;
    private int retrievedScans;
    private TaskStatus status;
    private String errorMessage;

    // The 3D display
    private DisplayImpl display;

    // data resolution on m/z and retention time axis
    private int resolutionMZ;

    // data resolution on retention time axis
    private int resolutionRT;

    // maximum value on Z axis
    private float maxBinnedIntensity;
    
    // number of labeled ticks on retention time axis
    private static final int X_AXIS_TICKS = 30;
    
    // number of labeled ticks on m/z axis
    private static final int Y_AXIS_TICKS = 30;
    
    // number of labeled ticks (marks) on intensity axis
    private static final int Z_AXIS_TICKS = 15;
    
    // number of minor (not labeled) axis ticks per 1 major tick
    private static final int MINOR_TICKS = 5;

    // tick labels font size
    private static final int LABEL_FONT_SIZE = 2;
    
    // maximum number of scans sampled 
    // if we have more scans, we have to bin them into retention time intervals
    private static final int MAXIMUM_SCANS = 2000;
    
    // maximum number of m/z bins 
    private static final int MAXIMUM_MZ_BINS = 800;
    
    // axes aspect ratio X:Y:Z
    private static final double[] ASPECT_RATIO = new double[] { 1, 0.8, 0.2 };
    
    
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
    ThreeDSamplingTask(RawDataFile rawDataFile, int msLevel,
            ThreeDVisualizer visualizer) {

        status = TaskStatus.WAITING;

        this.rawDataFile = rawDataFile;
        this.msLevel = msLevel;

        scanNumbers = rawDataFile.getScanNumbers(msLevel);

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
            final float mzRange = (float) (rawDataFile.getDataMaxMZ(msLevel) - rawDataFile.getDataMinMZ(msLevel));
            final float rtRange = (float) (rawDataFile.getDataMaxRT(msLevel) - rawDataFile.getDataMinRT(msLevel));

            
            // create 3D display
            display = new DisplayImplJ3D(rawDataFile.toString());

            // retention time data type
            // we have to use "RT", "Retention time" returns null(?)
            RealType retentionTimeType = RealType.getRealType("RT", SI.second);

            // m/z data type
            RealType mzType = RealType.getRealType("m/z");

            // intensity type
            RealType intensityType = RealType.getRealType("Intensity");

            // function domain - R^2 (retention time and m/z)
            RealTupleType domainTuple = new RealTupleType(retentionTimeType, mzType);

            
            // domain values set
            Set domainSet;

            // set the resolution (number of data points) on m/z axis
            resolutionMZ = Math.min(MAXIMUM_MZ_BINS, Math.round(mzRange));
            final float mzStep = mzRange / resolutionMZ;
            
            // set the resolution (number of data points) on retention time axis
            if (scanNumbers.length > MAXIMUM_SCANS) {
                
                // if the number of scans exceeds MAXIMUM_SCANS, we have to bin scans
                resolutionRT = MAXIMUM_SCANS;

                domainSet = new Linear2DSet(domainTuple,
                        rawDataFile.getDataMinRT(msLevel),
                        rawDataFile.getDataMaxRT(msLevel),
                        resolutionRT,
                        rawDataFile.getDataMinMZ(msLevel),
                        rawDataFile.getDataMaxMZ(msLevel),
                        resolutionMZ);

            } else {

                // number of scans is lower then MAXIMUM_SCANS, so we can create a grid column for each scan
                resolutionRT = scanNumbers.length;

                // domain points in 2D grid
                float domainPoints[][] = new float[2][resolutionMZ * resolutionRT];

                for (int j = 0; j < resolutionMZ; j++) {
                    for (int i = 0; i < resolutionRT; i++) {

                        // set the point's X coordinate
                        domainPoints[0][(resolutionRT * j) + i] = (float) rawDataFile.getRetentionTime(scanNumbers[i]);
                        
                        // set the point's Y coordinate
                        domainPoints[1][(resolutionRT * j) + i] = (float) rawDataFile.getDataMinMZ(msLevel) + (j * mzStep);
                    }
                }

                domainSet = new Gridded2DSet(domainTuple, domainPoints,
                        resolutionRT, resolutionMZ);

            }
            
            final float rtStep = rtRange / resolutionRT;

            // create an array for all data points
            float[][] intensityValues = new float[1][resolutionMZ * resolutionRT];
            
            // load scans
            Scan scan;
            int scanBinIndex;
            for (int scanIndex = 0; scanIndex < scanNumbers.length; scanIndex++) {

                if (status == TaskStatus.CANCELED)
                    return;

                scan = rawDataFile.getScan(scanNumbers[scanIndex]);

                double[] binnedIntensities = MyMath.binValues(scan.getMZValues(),
                        scan.getIntensityValues(),
                        rawDataFile.getDataMinMZ(msLevel),
                        rawDataFile.getDataMaxMZ(msLevel), 
                        resolutionMZ,
                        false,
                        BinningType.SUM);


                if (scanNumbers.length > MAXIMUM_SCANS) {
                    double rt = scan.getRetentionTime();
                    scanBinIndex = (int) ((rt - rawDataFile.getDataMinRT(msLevel)) / rtStep);
                    
                    // last scan falls into last bin
                    if (scanBinIndex == resolutionRT) scanBinIndex--;
                    
                } else {
                    // 1 scan per 1 grid column
                    scanBinIndex = scanIndex;
                }

                for (int mzIndex = 0; mzIndex < resolutionMZ; mzIndex++) {
                    
                    int intensityValuesIndex = (resolutionRT * mzIndex) + scanBinIndex;

                    if (binnedIntensities[mzIndex] > intensityValues[0][intensityValuesIndex])
                        intensityValues[0][intensityValuesIndex] = (float) binnedIntensities[mzIndex];

                    if (intensityValues[0][intensityValuesIndex] > maxBinnedIntensity)
                        maxBinnedIntensity = (float) binnedIntensities[mzIndex];
                }

                retrievedScans++;

            }

            
            // create a function from domain (retention time and m/z) to intensity
            FunctionType intensityFunction = new FunctionType(domainTuple, intensityType);

            // sampled Intensity values stored in 1D array (FlatField)
            FlatField intensityValuesFlatField = new FlatField(intensityFunction, domainSet);
            intensityValuesFlatField.setSamples(intensityValues, false);

            // create a DataReference connecting data to display
            DataReference dataReference = new DataReferenceImpl(rawDataFile.toString());
            dataReference.setData(intensityValuesFlatField);
            display.addReference(dataReference);

            
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
            ScalarMap colorMap = new ScalarMap(intensityType, Display.RGB);

            // add maps to display
            display.addMap(retentionTimeMap);
            display.addMap(mzMap);
            display.addMap(intensityMap);
            display.addMap(colorMap);

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
                MouseHelper.ZOOM       // SHIFT + right mouse button
                }, {
                MouseHelper.TRANSLATE, // CTRL + right mouse button
                MouseHelper.ZOOM       // CTRL + SHIFT + right mouse button
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

            
            // get projection control to set initial rotation and zooming
            ProjectionControl projCont = display.getProjectionControl();

            // set axes aspect ratio
            projCont.setAspect(ASPECT_RATIO);

            // get default projection matrix
            double[] pControlMatrix = projCont.getMatrix();

            // prepare rotation and scaling matrix
            double[] mult = MouseBehaviorJ3D.static_make_matrix(
                    75, 0, 0,   // rotation X,Y,Z
                    1,        // scaling
                    0.1, 0.2, 0 // translation (moving) X,Y,Z
                    );

            // multiply projection matrix
            pControlMatrix = MouseBehaviorJ3D.static_multiply_matrix(mult, pControlMatrix);

            // set new projection matrix
            projCont.setMatrix(pControlMatrix);
            

        } catch (Throwable e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        status = TaskStatus.FINISHED;

    }

}
