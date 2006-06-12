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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.CursorPosition;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Logger;
import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.util.RawDataAcceptor;
import net.sf.mzmine.util.RawDataRetrievalTask;
import net.sf.mzmine.util.TimeNumberFormat;
import net.sf.mzmine.util.MyMath.BinningType;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;
import visad.ContourControl;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRenderer;
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
import visad.java3d.MouseBehaviorJ3D;
import visad.util.HersheyFont;

// TODO: plotmaps? LOD?

/**
 * 3D visualizer using VisAd library
 */
public class ThreeDVisualizer extends JInternalFrame implements
        RawDataVisualizer, ActionListener, TaskListener, MouseWheelListener,
        RawDataAcceptor {

    private ThreeDToolBar toolBar;

    private RawDataFile rawDataFile;
    private int msLevel;

    // TODO: get these from parameter storage
    private static NumberFormat rtFormat = new TimeNumberFormat();
    private static NumberFormat mzFormat = new DecimalFormat("0.##");
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");

    private RealType rtType, mzType;
    private RealType intType;

    // Two Tuples: one to pack longitude and latitude together, as the domain
    // and the other for the range (altitude, temperature)

    private RealTupleType domain_tuple;

    // The function (domain_tuple -> range_tuple )

    private FunctionType func_domain_range;

    // Our Data values for the domain are represented by the Set

    private Set domain_set;

    // The Data class FlatField

    private FlatField vals_ff;

    // The DataReference from data to display

    private DataReferenceImpl data_ref;

    // The 3D display, and its the maps

    private DisplayImpl display;
    private ScalarMap latMap, lonMap;
    private ScalarMap altZMap, tempRGBMap;

    private int resol, resolScan;

    private float[][] flat_samples;

    private int scanNumbers[];

    private float maxBinnedIntensity;
    
    private static final int MAXRESRT = 2000;

    public ThreeDVisualizer(RawDataFile rawDataFile, int msLevel) {

        super(rawDataFile.toString(), true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.rawDataFile = rawDataFile;
        this.msLevel = msLevel;

        toolBar = new ThreeDToolBar(this);
        add(toolBar, BorderLayout.EAST);

        try {

            // Create the quantities
            // Use RealType(String name);

            rtType = RealType.getRealType("RT", SI.second);
            assert rtType != null;
            mzType = RealType.getRealType("m/z");

            domain_tuple = new RealTupleType(rtType, mzType);

            intType = RealType.getRealType("Intensity"); // ,
            // SI.kelvin.scale(0.0000001));
            // // ,
            // BaseUnit.addBaseUnit(
            // "Intensity", "intensity"), null);

            // Create the range tuple ( altitude, temperature )
            // Use RealTupleType( RealType[] )

            // range_tuple = new RealTupleType(intType, temperatureType);

            // Create a FunctionType (domain_tuple -> range_tuple )
            // Use FunctionType(MathType domain, MathType range)

            func_domain_range = new FunctionType(domain_tuple, intType);

            scanNumbers = rawDataFile.getScanNumbers(msLevel);

            resol = Math.min(800,(int) (rawDataFile.getDataMaxMZ(msLevel) - rawDataFile.getDataMinMZ(msLevel)));;
             
            if (scanNumbers.length > MAXRESRT) {
                resolScan = 1500; 
                
                domain_set = new Linear2DSet(domain_tuple, 
                rawDataFile.getDataMinRT(msLevel), rawDataFile.getDataMaxRT(msLevel),
                resolScan,
                rawDataFile.getDataMinMZ(msLevel), rawDataFile.getDataMaxMZ(msLevel),
                resol);
                
                
            }
                else {
            
                resolScan =scanNumbers.length;

                float xpoints[][] = new float[2][resol * resolScan];

                float step = (float) (rawDataFile.getDataMaxMZ(msLevel) - rawDataFile.getDataMinMZ(msLevel))
                        / resol;
                
                for (int j = 0; j < resol; j++) {
                    for (int i = 0; i < resolScan; i++) {
                        
                        xpoints[0][(resolScan * j) + i] = (float) rawDataFile.getRetentionTime(scanNumbers[i]);
                        xpoints[1][(resolScan * j) + i] = (float) rawDataFile.getDataMinMZ(msLevel)
                                + (j * step);
                    }
                }
                
                domain_set = new Gridded2DSet(domain_tuple, xpoints,
                        resolScan, resol);
                
            }

               

            // resol = (int) Math.round(rawDataFile.getDataMaxMZ(msLevel) -
            // rawDataFile.getDataMinMZ(msLevel));

            System.out.println("resol " + resolScan + "x" + resol);


            

            // xpoints[0] = new float[resol];
            // for (int i = 0; i < resol; i++) xpoints[0][i] =

            // Create the domain Set
            // Use LinearDSet(MathType type, double first1, double last1, int
            // lengthX,
            // double first2, double last2, int lengthY)

            // int NCOLS = 100;
            // int NROWS = NCOLS;
            

                    



            flat_samples = new float[1][resol * resolScan];

            Task updateTask = new RawDataRetrievalTask(rawDataFile,
                    scanNumbers, "Updating 3D visualizer of " + rawDataFile,
                    this);

            TaskController.getInstance().addTask(updateTask, TaskPriority.HIGH,
                    this);

            System.out.println("data loaded");

            vals_ff = new FlatField(func_domain_range, domain_set);

            display = new DisplayImplJ3D(rawDataFile.toString());
            // display = new DisplayImplJ2D("display2");

            DisplayRenderer dRenderer = display.getDisplayRenderer();
            dRenderer.setBackgroundColor(Color.white);
            dRenderer.setBoxOn(false);
            // dRenderer.setBoxColor(Color.black);
            dRenderer.setCursorColor(Color.red);
            dRenderer.setForegroundColor(Color.black);

            dRenderer.getMouseBehavior().getMouseHelper().setFunctionMap(
                    new int[][][] {
                            { { MouseHelper.ROTATE, MouseHelper.ZOOM },
                                    { MouseHelper.ROTATE, MouseHelper.ZOOM } },
                            { { MouseHelper.NONE, MouseHelper.NONE },
                                    { MouseHelper.NONE, MouseHelper.NONE } },
                            { { MouseHelper.TRANSLATE, MouseHelper.ZOOM },
                                    { MouseHelper.TRANSLATE, MouseHelper.ZOOM } } });

            // Get display's graphics mode control and draw scales

            GraphicsModeControl dispGMC = (GraphicsModeControl) display.getGraphicsModeControl();
            dispGMC.setScaleEnable(true);
            dispGMC.setTextureEnable(false);
            // dispGMC.setLineWidth(1.1f);
            // dispGMC.setProjectionPolicy(dispGMC.PARALLEL_PROJECTION);

            // Create the ScalarMaps: latitude to YAxis, longitude to XAxis and
            // altitude to ZAxis and temperature to RGB
            // Use ScalarMap(ScalarType scalar, DisplayRealType display_scalar)

            latMap = new ScalarMap(rtType, Display.XAxis);

            lonMap = new ScalarMap(mzType, Display.YAxis);

            latMap.getAxisScale().setNumberFormat(rtFormat);

            double ticks = Math.round((rawDataFile.getDataMaxRT(msLevel) - rawDataFile.getDataMinRT(msLevel)) / 30);
            latMap.getAxisScale().setMinorTickSpacing(ticks / 5);
            latMap.getAxisScale().setMajorTickSpacing(ticks);

            // latMap.getAxisScale().setAutoComputeTicks(true);

            latMap.getAxisScale().setFont(new HersheyFont("futural"));
            latMap.getAxisScale().setLabelSize(2);
            latMap.getAxisScale().setLabelAllTicks(true);
            // latMap.getAxisScale().createStandardLabels(rawDataFile.getDataMaxRT(msLevel),
            // rawDataFile.getDataMinRT(msLevel),0, 60);
            latMap.getAxisScale().setColor(Color.black);
            // latMap.getAxisScale().setGridLinesVisible(true);
            latMap.getAxisScale().setTitle("Retention time");

            lonMap.getAxisScale().setColor(Color.black);
            lonMap.getAxisScale().setLabelSize(2);
            lonMap.getAxisScale().setLabelAllTicks(true);
            lonMap.getAxisScale().setNumberFormat(mzFormat);
            // lonMap.getAxisScale().createStandardLabels(rawDataFile.getDataMaxMZ(msLevel),
            // rawDataFile.getDataMinMZ(msLevel),0, 50);

            ticks = Math.round((rawDataFile.getDataMaxMZ(msLevel) - rawDataFile.getDataMinMZ(msLevel)) / 30);

            lonMap.getAxisScale().setMinorTickSpacing(ticks / 5);
            lonMap.getAxisScale().setMajorTickSpacing(ticks);

            // Add maps to display

            display.addMap(latMap);
            display.addMap(lonMap);

            // altitude to z-axis and temperature to color

            altZMap = new ScalarMap(intType, Display.ZAxis);
            // altZMap.setRange(0,
            // rawDataFile.getDataMaxBasePeakIntensity(msLevel) / 2);

            altZMap.getAxisScale().setColor(Color.black);

            altZMap.getAxisScale().setLabelSize(2);
            altZMap.getAxisScale().setLabelAllTicks(true);
            altZMap.getAxisScale().setNumberFormat(intensityFormat);

            // tempRGBMap.setScaleEnable();
            // tempRGBMap.setRange(0,
            // rawDataFile.getDataMaxBasePeakIntensity(msLevel) / 2);
            // Add maps to display
            display.addMap(altZMap);

            // log scale???

            tempRGBMap = new ScalarMap(intType, Display.RGB);

            display.addMap(tempRGBMap);

            ProjectionControl projCont = display.getProjectionControl();
            double[] aspect = new double[] { 1.5, 1, 0.5 };

            projCont.setAspect(aspect);

            double[] pControlMatrix = projCont.getMatrix();

            for (int i = 0; i < pControlMatrix.length; i++)
                System.out.println(pControlMatrix[i]);

            double[] mult = MouseBehaviorJ3D.static_make_matrix(75, 0, 0, 0.8,
                    0.1, 0.2, 0);
            pControlMatrix = MouseBehaviorJ3D.static_multiply_matrix(mult,
                    pControlMatrix);

            projCont.setMatrix(pControlMatrix);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create application window and add display to window

        display.getComponent().setPreferredSize(new Dimension(700, 500));
        add(display.getComponent(), BorderLayout.CENTER);

        display.getComponent().setFocusable(true);
        display.getComponent().addMouseWheelListener(this);
        GUIUtils.registerKeyHandler((JComponent) display.getComponent(),
                KeyStroke.getKeyStroke("SPACE"), this, "AAA");


        updateTitle();

        pack();

        taskStarted(null); // TODO

        repaint();

    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        // twoDPlot.getPlot().getRangeAxis().setRange(mzMin, mzMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        // twoDPlot.getPlot().getDomainAxis().setRange(rtMin, rtMax);

    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        // do nothing
    }

    void updateTitle() {

        StringBuffer title = new StringBuffer();

        title.append(rawDataFile.toString());
        title.append(": 3D view");

        setTitle(title.toString());

        title.append(", MS");
        title.append(msLevel);

        // twoDPlot.setTitle(title.toString());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        System.out.println(event);
        if (command.equals("AAA")) {
            try {

                ScalarMap tempIsoMap;
                try {
                    
                     tempIsoMap = new ScalarMap( intType, Display.IsoContour );
                      display.addMap( tempIsoMap ); 
                      ContourControl isoControl =
                      (ContourControl) tempIsoMap.getControl(); 
                      float interval = maxBinnedIntensity / 100; //
                      // interval between lines 
                      float lowValue = maxBinnedIntensity / 100; // lowest value
                      float highValue = maxBinnedIntensity; // highest value 
                      float base = 0f; //
                      // higstarting at this base value
                      
                     isoControl.setContourInterval(interval, lowValue, highValue,
                     base); // isoControl.enableLabels(true);
                      
                      
                      //ContourWidget contourWid = new ContourWidget( tempIsoMap );
                      //add(contourWid, BorderLayout.SOUTH);
                      
                      
                     
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            };

        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            MainWindow.getInstance().displayErrorMessage(
                    "Error while updating 3D visualizer: "
                            + task.getErrorMessage());
        }

        if (task.getStatus() == TaskStatus.FINISHED) {

            try {

                double ticks = Math.round(maxBinnedIntensity / 15);
                altZMap.getAxisScale().setMinorTickSpacing(ticks / 5);
                altZMap.getAxisScale().setMajorTickSpacing(ticks);
                tempRGBMap.setRange(0, maxBinnedIntensity / 2);

                vals_ff.setSamples(flat_samples, false);
                data_ref = new DataReferenceImpl("data_ref");

                data_ref.setData(vals_ff);

                // Add reference to display

                display.addReference(data_ref);

            } catch (Exception e) {
                Logger.put(e.toString());
            }
            // if we have not added this frame before, do it now
            if (getParent() == null)
                MainWindow.getInstance().addInternalFrame(this);

        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {

    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#getCursorPosition()
     */
    public CursorPosition getCursorPosition() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setCursorPosition(net.sf.mzmine.util.CursorPosition)
     */
    public void setCursorPosition(CursorPosition newPosition) {
        // TODO Auto-generated method stub

    }

    /**
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    public void mouseWheelMoved(MouseWheelEvent event) {

        int rot = event.getWheelRotation();
        try {
            ProjectionControl pControl = display.getProjectionControl();
            double[] pControlMatrix = pControl.getMatrix();

            double scale;
            if (rot < 0)
                scale = 1.03;
            else
                scale = 0.97;
            double[] mult = MouseBehaviorJ3D.static_make_matrix(0.0, 0.0, 0.0,
                    scale, 0.0, 0.0, 0.0);

            double newm[] = MouseBehaviorJ3D.static_multiply_matrix(mult,
                    pControlMatrix);

            pControl.setMatrix(newm);
            pControl.saveProjection();
        } catch (Exception re) {
        }

    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#addScan(net.sf.mzmine.interfaces.Scan,
     *      int)
     */
    public void addScan(Scan scan, int index) {

        double[] ints = MyMath.binValues(scan.getMZValues(),
                scan.getIntensityValues(), rawDataFile.getDataMinMZ(msLevel),
                rawDataFile.getDataMaxMZ(msLevel), resol, !scan.isCentroided(),
                BinningType.SUM);

        float stepRT = (float) (rawDataFile.getDataMaxRT(msLevel) - rawDataFile.getDataMinRT(msLevel))
        / resolScan;
        
        double rt = scan.getRetentionTime();
        
        int bin;
        
        if (scanNumbers.length > MAXRESRT) {
        bin = (int) ((rt - rawDataFile.getDataMinRT(msLevel)) / stepRT);
        if (bin == resolScan) bin--;
        } else bin = index;
        
        
        for (int j = 0; j < resol; j++) {
            int ind = (resolScan * j) + bin;

            if (ints[j] > flat_samples[0][ind]) flat_samples[0][ind] = (float) ints[j];
            
            if (flat_samples[0][ind]  > maxBinnedIntensity)
                maxBinnedIntensity = (float) ints[j];
        }
    }

}