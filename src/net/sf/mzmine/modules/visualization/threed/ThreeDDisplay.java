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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.threed;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.text.NumberFormat;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import visad.AxisScale;
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
 * Visad's DisplayImplJ3D modified for our purpose
 */
class ThreeDDisplay extends DisplayImplJ3D {

    private static final Font annotationFont = new Font("SansSerif",
            Font.PLAIN, 8);

    // axes aspect ratio X:Y:Z
    private static final double[] ASPECT_RATIO = new double[] { 1, 0.8, 0.3 };

    private RealType retentionTimeType, mzType, intensityType;

    // peak height is for picked peaks, same as intensity, but not
    // mapped to color
    private RealType peakHeightType;

    // [X:Y:Z] tuple for drawing peak box
    private RealTupleType pointTupleType;

    // function domain - R^2 (retention time and m/z)
    private RealTupleType domainTuple;

    // annotation type
    private TextType annotationType;

    // annotation range
    private TupleType annotationTupleType;

    // peak area renderer
    private PickManipulationRendererJ3D pickRenderer;

    // peak areas
    private ThreeDPeakCells cells;

    // functions domain -> range
    private FunctionType intensityFunction, annotationFunction;

    private ScalarMap retentionTimeMap, mzMap, intensityMap, heightMap,
            colorMap, annotationMap;

    // data references
    private DataReference dataReference, peaksReference;

    // axes
    private AxisScale retentionTimeAxis, mzAxis, intensityAxis;

    private double maxIntensity;

    private boolean peaksShown;

    private ConstantMap[] peakColorMap;

    ThreeDDisplay() throws RemoteException, VisADException {

        super("display");

        // Setup data types
        peakHeightType = RealType.getRealType("Height");
        retentionTimeType = RealType.getRealType("RT", SI.second);
        mzType = RealType.getRealType("m/z");
        intensityType = RealType.getRealType("Intensity");
        annotationType = TextType.getTextType("Annotation");
        pointTupleType = new RealTupleType(retentionTimeType, mzType,
                peakHeightType);
        domainTuple = new RealTupleType(retentionTimeType, mzType);
        annotationTupleType = new TupleType(new MathType[] { peakHeightType,
                annotationType });

        // Create a function from domain (retention time and m/z) to
        // intensity
        intensityFunction = new FunctionType(domainTuple, intensityType);

        // Create a function from domain (retention time and m/z) to text
        // annotation
        annotationFunction = new FunctionType(domainTuple, annotationTupleType);

        // create a DataReference connecting data to display
        dataReference = new DataReferenceImpl("data");

        // create mapping for X,Y,Z axes and color
        retentionTimeMap = new ScalarMap(retentionTimeType, Display.XAxis);
        mzMap = new ScalarMap(mzType, Display.YAxis);
        intensityMap = new ScalarMap(intensityType, Display.ZAxis);
        heightMap = new ScalarMap(peakHeightType, Display.ZAxis);
        colorMap = new ScalarMap(intensityType, Display.RGB);
        annotationMap = new ScalarMap(annotationType, Display.Text);

        // Add maps to display
        addMap(retentionTimeMap);
        addMap(mzMap);
        addMap(intensityMap);
        addMap(heightMap);
        addMap(colorMap);
        addMap(annotationMap);

        // Get formatters
        NumberFormat rtFormat = MZmineCore.getRTFormat();
        NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

        // set retention time axis properties
        retentionTimeAxis = retentionTimeMap.getAxisScale();
        retentionTimeAxis.setColor(Color.black);
        retentionTimeAxis.setTitle("Retention time");
        retentionTimeAxis.setLabelAllTicks(true);
        retentionTimeAxis.setNumberFormat(rtFormat);
        retentionTimeAxis.setFont(annotationFont);

        // set m/z axis properties
        // we ignore mzformat because it ends up like 400.00000 anyway
        mzAxis = mzMap.getAxisScale();
        mzAxis.setColor(Color.black);
        mzAxis.setLabelAllTicks(true);
        mzAxis.setFont(annotationFont);

        // set intensity axis properties
        intensityAxis = intensityMap.getAxisScale();
        intensityAxis.setColor(Color.black);
        intensityAxis.setLabelAllTicks(true);
        intensityAxis.setNumberFormat(intensityFormat);
        intensityAxis.setFont(annotationFont);

        // height is the same as intensity
        AxisScale peakHeightAxis = heightMap.getAxisScale();
        peakHeightAxis.setVisible(false);

        // get graphics mode control to set axes and textures properties
        GraphicsModeControl dispGMC = getGraphicsModeControl();

        // show axis scales
        dispGMC.setScaleEnable(true);

        // no textures
        dispGMC.setTextureEnable(false);

        // get display renderer to set colors, box, mouse, keys..
        DisplayRendererJ3D dRenderer = (DisplayRendererJ3D) getDisplayRenderer();

        // set colors
        dRenderer.setForegroundColor(Color.black);
        dRenderer.setBackgroundColor(Color.white);

        // do not show box around the data
        dRenderer.setBoxOn(false);

        // set the mouse behavior
        int mouseBehavior[][][] = new int[][][] { { { MouseHelper.ROTATE, // left
                                                                            // mouse
                                                                            // button
                MouseHelper.ZOOM // SHIFT + left mouse button
                }, { MouseHelper.ROTATE, // CTRL + left mouse button
                        MouseHelper.ZOOM // CTRL + SHIFT + left mouse button
                } }, { { MouseHelper.NONE, // middle mouse button
                MouseHelper.NONE // SHIFT + middle mouse button
                }, { MouseHelper.NONE, // CTRL + middle mouse button
                        MouseHelper.NONE // CTRL + SHIFT + middle mouse
                // button
                } }, { { MouseHelper.TRANSLATE, // right mouse button
                MouseHelper.DIRECT // SHIFT + right mouse button
                }, { MouseHelper.TRANSLATE, // CTRL + right mouse button
                        MouseHelper.DIRECT // CTRL + SHIFT + right mouse button
                } } };
        dRenderer.getMouseBehavior().getMouseHelper().setFunctionMap(
                mouseBehavior);

        // set the keyboard behavior
        KeyboardBehaviorJ3D keyBehavior = new KeyboardBehaviorJ3D(dRenderer);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_X_POS,
                KeyEvent.VK_DOWN, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_X_NEG,
                KeyEvent.VK_UP, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Y_POS,
                KeyEvent.VK_LEFT, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Y_NEG,
                KeyEvent.VK_RIGHT, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Z_POS,
                KeyEvent.VK_PAGE_UP, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ROTATE_Z_NEG,
                KeyEvent.VK_PAGE_DOWN, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ZOOM_IN,
                KeyEvent.VK_PLUS, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ZOOM_OUT,
                KeyEvent.VK_MINUS, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ZOOM_IN,
                KeyEvent.VK_ADD, 0);
        keyBehavior.mapKeyToFunction(KeyboardBehaviorJ3D.ZOOM_OUT,
                KeyEvent.VK_SUBTRACT, 0);
        dRenderer.addKeyboardBehavior(keyBehavior);

        // set text control properties
        TextControl textControl = (TextControl) annotationMap.getControl();
        textControl.setCenter(true);
        textControl.setAutoSize(false);
        textControl.setScale(0.3);
        textControl.setFont(annotationFont);

        // get projection control to set initial rotation and zooming
        ProjectionControl projCont = getProjectionControl();

        // set axes aspect ratio
        projCont.setAspect(ASPECT_RATIO);

        // get default projection matrix
        double[] pControlMatrix = projCont.getMatrix();

        // prepare rotation and scaling matrix
        double[] mult = MouseBehaviorJ3D.static_make_matrix(75, 0, 0, // rotation
                                                                        // X,Y,Z
                1, // scaling
                0.1, 0.2, 0 // translation (moving) X,Y,Z
        );

        // multiply projection matrix
        pControlMatrix = MouseBehaviorJ3D.static_multiply_matrix(mult,
                pControlMatrix);

        // set new projection matrix
        projCont.setMatrix(pControlMatrix);

        // color of text annotations
        peakColorMap = new ConstantMap[] { new ConstantMap(1, Display.Red),
                new ConstantMap(1, Display.Green),
                new ConstantMap(0.0, Display.Blue) };

        // create a pick renderer, so we can track user clicks
        pickRenderer = new PickManipulationRendererJ3D();

        // data reference for peaks
        peaksReference = new DataReferenceImpl("peaks");
        peaksShown = false;

        // peak area cells
        cells = new ThreeDPeakCells(this);

    }

    /**
     * Set data points
     */
    void setData(double intensityValues[][], Set domainSet, double rtMin,
            double rtMax, double mzMin, double mzMax, double maxIntensity) {

        try {

            // sampled Intensity values stored in 1D array (FlatField)
            FlatField intensityValuesFlatField = new FlatField(
                    intensityFunction, domainSet);
            intensityValuesFlatField.setSamples(intensityValues, false);

            this.maxIntensity = maxIntensity;

            retentionTimeMap.setRange(rtMin, rtMax);
            mzMap.setRange(mzMin, mzMax);
            intensityMap.setRange(0, maxIntensity);
            heightMap.setRange(0, maxIntensity);

            // set the color axis top intensity to 20% of the maximum intensity
            // value, because the peaks are usually sharp
            colorMap.setRange(0, maxIntensity / 5);

            dataReference.setData(intensityValuesFlatField);
            addReference(dataReference);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Set picked peaks
     */
    void setPeaks(PeakList peakList, ChromatographicPeak peaks[],
            boolean showCompoundName) {

        try {

            float peaksDomainPoints[][] = new float[2][peaks.length];
            Data peakValues[] = new Data[peaks.length];

            // set the resolution (number of data points) on m/z axis

            NumberFormat mzFormat = MZmineCore.getMZFormat();
            for (int i = 0; i < peaks.length; i++) {

                peaksDomainPoints[0][i] = (float) peaks[i].getRT();
                peaksDomainPoints[1][i] = (float) peaks[i].getMZ();

                Data[] peakData = new Data[2];
                peakData[0] = new Real(peakHeightType, peaks[i].getHeight()
                        + (maxIntensity * 0.03));

                String peakText;

                PeakListRow row = peakList.getPeakRow(peaks[i]);
                PeakIdentity id = row.getPreferredCompoundIdentity();
                if (showCompoundName && (id != null))
                    peakText = id.getName();
                else
                    peakText = mzFormat.format(peaks[i].getMZ());
                peakData[1] = new Text(annotationType, peakText);

                peakValues[i] = new Tuple(annotationTupleType, peakData, false);

            }

            // peak domain points set
            Set peaksDomainSet = new Gridded2DSet(domainTuple,
                    peaksDomainPoints, peaks.length);

            // create peak values flat field
            FieldImpl peakValuesFlatField = new FieldImpl(annotationFunction,
                    peaksDomainSet);
            peakValuesFlatField.setSamples(peakValues, false);

            peaksReference.setData(peakValuesFlatField);

            // We have to remove the reference and add it again because the data
            // have changed
            if (peaksShown) {
                cells.removeReference(peaksReference);
            }

            cells.addReference(peaksReference);

            if (!peaksShown) {
                addReferences(pickRenderer, peaksReference, peakColorMap);
                peaksShown = true;
            }

            cells.setPeaks(peaks);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Toggle whether peaks are annotated or not. Annotation requires setPeaks()
     * call first.
     * 
     */
    void toggleShowingPeaks() {

        try {
            if (peaksShown) {
                removeReference(peaksReference);
                peaksShown = false;
            } else {
                addReferences(pickRenderer, peaksReference, peakColorMap);
                peaksShown = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    RealTupleType getPointTupleType() {
        return pointTupleType;
    }

    PickManipulationRendererJ3D getPickRenderer() {
        return pickRenderer;
    }

    RealTupleType getDomainTuple() {
        return domainTuple;
    }

}
