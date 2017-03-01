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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Iterator;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import visad.AxisScale;
import visad.BaseColorControl;
import visad.CommonUnit;
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
import visad.KeyboardBehavior;
import visad.LogCoordinateSystem;
import visad.MathType;
import visad.MouseHelper;
import visad.ProjectionControl;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ReferenceException;
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
 * VisAD's DisplayImplJ3D modified for our purposes.
 */
public class ThreeDDisplay extends DisplayImplJ3D {

    // Annotations font.
    private static final Font ANNOTATION_FONT = new Font("SansSerif",
	    Font.PLAIN, 8);

    // Axes aspect ratio X:Y:Z.
    private static final double[] ASPECT_RATIO = { 1.0, 0.8, 0.3 };

    // Mouse interaction transform.
    private static final double[] MOUSE_BEHAVIOUR_MATRIX = MouseBehaviorJ3D
	    .static_make_matrix(75.0, 0.0, 0.0, // rotation: X,Y,Z
		    1.0, // scaling
		    0.1, 0.2, 0.0 // translation: X,Y,Z
	    );

    // color table length.
    private static final int COLOR_TABLE_LENGTH = 20;

    // Text scale.
    private static final double TEXT_SCALE = 0.3;

    // Peak label offset.
    private static final double PEAK_LABEL_OFFSET = 0.03;

    private final RealTupleType heightTupleType;

    // Function domain - R^2 (retention time and m/z).
    private final RealTupleType domainTuple;

    // Annotation type.
    private final TextType annotationType;

    // Annotation range.
    private final TupleType annotationTupleType;

    // Peak area renderer.
    private final PickManipulationRendererJ3D pickRenderer;

    // Peak areas.
    private final ThreeDPeakCells cells;

    // functions domain -> range.
    private final FunctionType intensityFunction;
    private final FunctionType annotationFunction;

    // Scalar maps.
    private final ScalarMap retentionTimeMap;
    private final ScalarMap mzMap;
    private final ScalarMap intensityMap;
    private final ScalarMap logIntensityMap;
    private final ScalarMap heightMap;
    private final ScalarMap logHeightMap;
    private final ScalarMap colorMap;
    private final ScalarMap annotationAlphaMap;

    // Peak color map.
    private final ConstantMap[] peakColorMap;

    // Data references.
    private final DataReference dataReference;
    private final DataReference peaksReference;

    // Intensity statistics.
    private double maxIntensity;

    // Whether to show peaks.
    private boolean peaksShown;

    // Use log10 scaling.
    private boolean useLog10Intensity;

    /**
     * Creates the display.
     * 
     * @throws RemoteException
     *             if there are problems creating the display.
     * @throws VisADException
     *             if there are problems creating the display.
     */
    public ThreeDDisplay() throws RemoteException, VisADException {

	super("display");

	// Initialization.
	maxIntensity = 0.0;
	useLog10Intensity = false;

	// Domain.
	final RealType retentionTimeType = RealType
		.getRealType("RT", SI.second);
	final RealType mzType = RealType.getRealType("m/z");
	domainTuple = new RealTupleType(retentionTimeType, mzType);

	// Intensity types.
	final RealType intensityType = RealType.getRealType("Intensity");
	final RealType logIntensityType = RealType.getRealType("LogIntensity",
		CommonUnit.promiscuous);
	final RealTupleType intensityRange = new RealTupleType(intensityType,
		new LogCoordinateSystem(new RealTupleType(logIntensityType)),
		null);

	// Height Types.
	final RealType heightType = RealType.getRealType("Height");
	final RealType logHeightType = RealType.getRealType("LogHeight",
		CommonUnit.promiscuous);
	heightTupleType = new RealTupleType(heightType,
		new LogCoordinateSystem(new RealTupleType(logHeightType)), null);
	annotationType = TextType.getTextType("Annotation");
	annotationTupleType = new TupleType(new MathType[] { heightTupleType,
		annotationType });

	// Create a function from domain (retention time and m/z) to intensity.
	intensityFunction = new FunctionType(domainTuple, intensityRange);

	// Create a function from domain (retention time and m/z) to text
	// annotation.
	annotationFunction = new FunctionType(domainTuple, annotationTupleType);

	// Create a DataReference connecting data to display.
	dataReference = new DataReferenceImpl("data");

	// Create mapping for X,Y,Z axes and color.
	retentionTimeMap = new ScalarMap(retentionTimeType, Display.XAxis);
	mzMap = new ScalarMap(mzType, Display.YAxis);
	intensityMap = new ScalarMap(intensityType, Display.ZAxis);
	logIntensityMap = new ScalarMap(logIntensityType, Display.ZAxis);
	heightMap = new ScalarMap(heightType, Display.ZAxis);
	logHeightMap = new ScalarMap(logHeightType, Display.ZAxis);
	colorMap = new ScalarMap(intensityType, Display.RGB);
	annotationAlphaMap = new ScalarMap(heightType, Display.Alpha);
	final ScalarMap annotationMap = new ScalarMap(annotationType,
		Display.Text);

	// Add maps to display.
	addMap(retentionTimeMap);
	addMap(mzMap);
	addMap(colorMap);
	addMap(annotationMap);
	addMap(annotationAlphaMap);

	// Set color map.
	((BaseColorControl) colorMap.getControl()).setTable(createColorTable());

	// Set retention time axis properties. We do not use the RT format from
	// MZmine configuration, because VisAD is quite smart to choose the
	// right format dynamically depending on the scale of the values.
	configureAxis(retentionTimeMap.getAxisScale(), "Retention Time", null);

	// Set m/z axis properties: We do not use the m/z format from
	// MZmine configuration, because VisAD is quite smart to choose the
	// right format dynamically depending on the scale of the values.
	configureAxis(mzMap.getAxisScale(), "m/z", null);

	// Set intensity axis properties.
	configureAxis(intensityMap.getAxisScale(), "Intensity", MZmineCore
		.getConfiguration().getIntensityFormat());

	// Set log axis properties.
	configureAxis(logIntensityMap.getAxisScale(), "Intensity", MZmineCore
		.getConfiguration().getIntensityFormat());

	// height is the same as intensity
	heightMap.getAxisScale().setVisible(false);
	logHeightMap.getAxisScale().setVisible(false);

	// Configure graphics mode control to set axes and textures properties.
	final GraphicsModeControl graphicsModeControl = getGraphicsModeControl();
	graphicsModeControl.setScaleEnable(true);
	graphicsModeControl.setTextureEnable(false);

	// Configure display renderer to set colors, box, mouse, keys.
	final DisplayRendererJ3D dRenderer = (DisplayRendererJ3D) getDisplayRenderer();
	dRenderer.setForegroundColor(Color.black);
	dRenderer.setBackgroundColor(Color.white);
	dRenderer.setBoxOn(false);
	dRenderer.getMouseBehavior().getMouseHelper()
		.setFunctionMap(new int[][][] { { { MouseHelper.ROTATE, // left
									// mouse
									// button
			MouseHelper.ZOOM // SHIFT + left mouse button
			}, { MouseHelper.ROTATE, // CTRL + left mouse button
				MouseHelper.ZOOM // CTRL + SHIFT + left mouse
						 // button
			} }, { { MouseHelper.NONE, // middle mouse button
			MouseHelper.NONE // SHIFT + middle mouse button
			}, { MouseHelper.NONE, // CTRL + middle mouse button
				MouseHelper.NONE // CTRL + SHIFT + middle mouse
						 // button
			} }, { { MouseHelper.TRANSLATE, // right mouse button
			MouseHelper.DIRECT // SHIFT + right mouse button
			}, { MouseHelper.TRANSLATE, // CTRL + right mouse button
				MouseHelper.DIRECT // CTRL + SHIFT + right mouse
						   // button
			} } });

	// Set the keyboard behavior.
	final KeyboardBehaviorJ3D keyBehavior = new KeyboardBehaviorJ3D(
		dRenderer);
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
	keyBehavior.mapKeyToFunction(KeyboardBehavior.ZOOM_IN,
		KeyEvent.VK_PLUS, 0);
	keyBehavior.mapKeyToFunction(KeyboardBehavior.ZOOM_OUT,
		KeyEvent.VK_MINUS, 0);
	keyBehavior.mapKeyToFunction(KeyboardBehavior.ZOOM_IN, KeyEvent.VK_ADD,
		0);
	keyBehavior.mapKeyToFunction(KeyboardBehavior.ZOOM_OUT,
		KeyEvent.VK_SUBTRACT, 0);
	dRenderer.addKeyboardBehavior(keyBehavior);

	// Set text control properties.
	final TextControl textControl = (TextControl) annotationMap
		.getControl();
	textControl.setCenter(true);
	textControl.setAutoSize(false);
	textControl.setScale(TEXT_SCALE);
	textControl.setFont(ANNOTATION_FONT);

	// Get projection control to set initial rotation and zooming.
	final ProjectionControl projCont = getProjectionControl();
	projCont.setAspect(ASPECT_RATIO);
	projCont.setMatrix(MouseBehaviorJ3D.static_multiply_matrix(
		MOUSE_BEHAVIOUR_MATRIX, projCont.getMatrix()));

	// Color of text annotations.
	peakColorMap = new ConstantMap[] { new ConstantMap(0.5, Display.Red),
		new ConstantMap(0.0, Display.Green),
		new ConstantMap(0.0, Display.Blue) };

	// Create a pick renderer, so we can track user clicks.
	pickRenderer = new PickManipulationRendererJ3D();

	// Data reference for peaks.
	peaksReference = new DataReferenceImpl("peaks");
	peaksShown = false;

	// Peak area cells.
	cells = new ThreeDPeakCells(this, new RealTupleType(retentionTimeType,
		mzType, heightType), pickRenderer);
    }

    /**
     * Gets the data set's maximum intensity.
     * 
     * @return the maximum.
     */
    public double getMaxIntensity() {
	return maxIntensity;
    }

    /**
     * Set data points.
     * 
     * @param intensityValues
     *            intensity values.
     * @param domainSet
     *            domain.
     * @param rtMin
     *            RT minimum.
     * @param rtMax
     *            RT maximum.
     * @param mzMin
     *            m/z minimum.
     * @param mzMax
     *            m/z maximum.
     * @param intensityMax
     *            intensity maximum.
     * @throws RemoteException
     *             if there are VisAD problems.
     * @throws VisADException
     *             if there are VisAD problems.
     */
    public void setData(final float[][] intensityValues, final Set domainSet,
	    final double rtMin, final double rtMax, final double mzMin,
	    final double mzMax, final double intensityMax)
	    throws VisADException, RemoteException {

	// Sampled intensity values stored in 1D array (FlatField).
	final FlatField flatField = new FlatField(intensityFunction, domainSet);
	flatField.setSamples(intensityValues, false);
	dataReference.setData(flatField);

	maxIntensity = intensityMax;
	retentionTimeMap.setRange(rtMin, rtMax);
	mzMap.setRange(mzMin, mzMax);
	addReference(dataReference);

	// Set the scaling mode.
	setUseLog10Intensity(useLog10Intensity);
    }

    /**
     * Set picked peaks.
     * 
     * @param peakList
     *            peak-list.
     * @param peaks
     *            peaks.
     * @param showCompoundName
     *            whether to show compound name.
     * @throws RemoteException
     *             if there are VisAD problems.
     * @throws VisADException
     *             if there are VisAD problems.
     */
    public void setPeaks(final PeakList peakList, final Feature[] peaks,
	    final boolean showCompoundName) throws RemoteException,
	    VisADException {

	final int peakCount = peaks.length;
	final float[][] peaksDomainPoints = new float[2][peakCount];
	final Data[] peakValues = new Data[peakCount];

	// Set the resolution (number of data points) on m/z axis.
	final NumberFormat mzFormat = MZmineCore.getConfiguration()
		.getMZFormat();
	for (int i = 0; i < peakCount; i++) {

	    peaksDomainPoints[0][i] = (float) peaks[i].getRT();
	    peaksDomainPoints[1][i] = (float) peaks[i].getMZ();

	    final Data[] peakData = new Data[2];
	    peakData[0] = new RealTuple(
		    heightTupleType,
		    new double[] { peaks[i].getRawDataPointsIntensityRange()
			    .upperEndpoint() + maxIntensity * PEAK_LABEL_OFFSET });

	    final PeakIdentity id = peakList.getPeakRow(peaks[i])
		    .getPreferredPeakIdentity();
	    final String peakText = showCompoundName && id != null ? id
		    .getName() : mzFormat.format(peaks[i].getMZ());
	    peakData[1] = new Text(annotationType, peakText);
	    peakValues[i] = new Tuple(annotationTupleType, peakData, false);
	}

	// Peak domain points set.
	final Set peaksDomainSet = new Gridded2DSet(domainTuple,
		peaksDomainPoints, peakCount);

	// Create peak values flat field.
	final FieldImpl peakValuesFlatField = new FieldImpl(annotationFunction,
		peaksDomainSet);
	peakValuesFlatField.setSamples(peakValues, false);

	peaksReference.setData(peakValuesFlatField);

	// We have to remove the reference and add it again because the data
	// have changed.
	if (peaksShown) {
	    cells.removeReference(peaksReference);
	}
	try {
	    cells.addReference(peaksReference);
	} catch (ReferenceException re) {

	    // ignored.
	}

	if (!peaksShown) {
	    addReferences(pickRenderer, peaksReference, peakColorMap);
	    peaksShown = true;
	}

	cells.setPeaks(peaks);
    }

    /**
     * Toggle whether peaks are annotated or not. Annotation requires setPeaks()
     * call first.
     * 
     * @throws RemoteException
     *             if there are VisAD problems.
     * @throws VisADException
     *             if there are VisAD problems.
     */
    public void toggleShowingPeaks() throws VisADException, RemoteException {

	if (peaksShown) {
	    removeReference(peaksReference);
	    peaksShown = false;
	} else {
	    addReferences(pickRenderer, peaksReference, peakColorMap);
	    peaksShown = true;
	}
    }

    public RealTupleType getDomainTuple() {
	return domainTuple;
    }

    /**
     * Gets the annotation color.
     * 
     * @return the color.
     */
    public Color getAnnotationColor() {

	return new Color((float) peakColorMap[0].getConstant(),
		(float) peakColorMap[1].getConstant(),
		(float) peakColorMap[2].getConstant());
    }

    /**
     * Sets the annotation color.
     * 
     * @param clr
     *            the new color.
     * @throws RemoteException
     *             if there are VisAD problems.
     * @throws VisADException
     *             if there are VisAD problems.
     */
    public void setAnnotationColor(final Color clr) throws VisADException,
	    RemoteException {

	final float[] rgb = clr.getRGBColorComponents(null);
	peakColorMap[0] = new ConstantMap(rgb[0], Display.Red);
	peakColorMap[1] = new ConstantMap(rgb[1], Display.Green);
	peakColorMap[2] = new ConstantMap(rgb[2], Display.Blue);
	if (peaksShown) {
	    removeReference(peaksReference);
	    addReferences(pickRenderer, peaksReference, peakColorMap);
	}
    }

    /**
     * Whether the annotation alpha map is in effect.
     * 
     * @return true if the annotation map is added to this display, false
     *         otherwise.
     */
    public boolean getUseAnnotationAlphaMap() {
	return equals(annotationAlphaMap.getDisplay());
    }

    /**
     * Enables/disables the annotation alpha map.
     * 
     * @param useMap
     *            whether to apply the map.
     * @throws RemoteException
     *             if there are VisAD problems.
     * @throws VisADException
     *             if there are VisAD problems.
     */
    public void setUseAnnotationAlphaMap(final boolean useMap)
	    throws VisADException, RemoteException {
	if (useMap) {
	    addMap(annotationAlphaMap);
	} else {
	    safeRemoveMap(annotationAlphaMap);
	}
    }

    /**
     * Is log10 scaling being used?
     * 
     * @return whether log10 scaling of the intensity is in use.
     */
    public boolean getUseLog10Intensity() {
	return useLog10Intensity;
    }

    /**
     * Sets whether to apply log10 scaling to the intensity axis.
     * 
     * @param useLog10
     *            whether to use log10 scaling.
     * @throws RemoteException
     *             if there are VisAD problems.
     * @throws VisADException
     *             if there are VisAD problems.
     */
    public void setUseLog10Intensity(final boolean useLog10)
	    throws VisADException, RemoteException {

	useLog10Intensity = useLog10;

	// Update maps.
	if (useLog10Intensity) {
	    safeRemoveMap(intensityMap);
	    safeRemoveMap(heightMap);
	    addMap(logIntensityMap);
	    addMap(logHeightMap);
	} else {
	    safeRemoveMap(logIntensityMap);
	    safeRemoveMap(logHeightMap);
	    addMap(intensityMap);
	    addMap(heightMap);
	}

	// Reset range.
	setIntensityRange(0.0, maxIntensity);
    }

    /**
     * Sets the intensity range.
     * 
     * @param min
     *            minimum intensity.
     * @param max
     *            maximum intensity
     * @throws VisADException
     *             if there are problems with VisAD.
     * @throws RemoteException
     *             if there are problems with VisAD.
     */
    public void setIntensityRange(final double min, final double max)
	    throws VisADException, RemoteException {

	// Set range.
	intensityMap.setRange(min, max);
	heightMap.setRange(min, max);
	colorMap.setRange(min, max);
	annotationAlphaMap.setRange(min, max);

	final double logMin = log10(min);
	final double logMax = log10(max);
	logIntensityMap.setRange(logMin, logMax);
	logHeightMap.setRange(logMin, logMax);

	// Relabel log axes.
	if (getUseLog10Intensity()) {
	    labelLogAxis();
	}
    }

    /**
     * Label the log axis.
     * 
     * @throws VisADException
     *             if there are problems with VisAD.
     */
    private void labelLogAxis() throws VisADException {

	// Get axis scale.
	final AxisScale scale = logIntensityMap.getAxisScale();
	final NumberFormat labelFormat = scale.getNumberFormat();

	// Create label table.
	final int maxLog = (int) Math.ceil(logIntensityMap.getRange()[1]) + 1;
	final Hashtable<Double, String> labelTable = new Hashtable<Double, String>(
		maxLog + 1);
	for (int i = 0; i <= maxLog; i++) {

	    // Add label.
	    labelTable.put((double) i,
		    labelFormat.format(StrictMath.pow(10.0, (double) i)));
	}

	scale.setLabelTable(labelTable);
    }

    /**
     * Calculate log10 of a number.
     * 
     * @param v
     *            the number.
     * @return log10(v) or 0.0 if v <= 0.
     */
    private static double log10(final double v) {
	return v > 0.0 ? StrictMath.log10(v) : 0.0;
    }

    /**
     * Removes a map if the display has it.
     * 
     * @param map
     *            the map to remove.
     * @throws VisADException
     *             if there are VisAD problems.
     * @throws RemoteException
     *             if there are VisAD problems.
     */
    private void safeRemoveMap(final ScalarMap map) throws VisADException,
	    RemoteException {

	// Is the map added?
	boolean hasMap = false;
	for (Iterator<?> iterator = getMapVector().iterator(); !hasMap
		&& iterator.hasNext();) {
	    hasMap = map.equals(iterator.next());
	}

	if (hasMap) {
	    removeMap(map);
	}
    }

    /**
     * Configure an axis.
     * 
     * @param axis
     *            the axis to configure.
     * @param title
     *            axis title.
     * @param format
     *            axis number format.
     */
    private static void configureAxis(final AxisScale axis, final String title,
	    final NumberFormat format) {

	axis.setColor(Color.black);
	axis.setTitle(title);
	axis.setLabelAllTicks(true);
	axis.setNumberFormat(format);
	axis.setFont(ANNOTATION_FONT);
	axis.setSnapToBox(true);
    }

    /**
     * Creates the color table.
     * 
     * @return the new color
     */
    private static float[][] createColorTable() {
	final float[][] table = new float[3][COLOR_TABLE_LENGTH];
	table[0][0] = 1.0f;
	table[1][0] = 1.0f;
	for (int i = 0; i < COLOR_TABLE_LENGTH; i++) {
	    table[2][i] = 1.0f;
	}
	return table;
    }
}
