/* 
 * Copyright (C) 2017 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV1_5;

import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.workflow.TwoStepDecomposition;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.GUIUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class ADAP3DecompositionV1_5SetupDialog extends ParameterSetupDialog 
{
    private static final Dimension MIN_DIMENSIONS = new Dimension(400, 300);
    private static final Font COMBO_FONT = new Font("SansSerif", Font.PLAIN,10);
    private static final DecimalFormat DECIMAL = new DecimalFormat("#.00");
    
    private static final byte NO_CHANGE = 0;
    private static final byte FIRST_PHASE_CHANGE = 1;
    private static final byte SECOND_PHASE_CHANGE = 2;
    
    class ComboClustersItem {
        public final List <PeakInfo> cluster;
        public final double aveRetTime;
        public final Map <PeakInfo, NavigableMap <Double, Double>> chromatograms;
        
        public ComboClustersItem(List <PeakInfo> cluster,
                Map <PeakInfo, NavigableMap <Double, Double>> chromatograms) 
        {
            this.cluster = cluster;
            this.chromatograms = chromatograms;
            
            double sumRetTime = 0.0;
            for (PeakInfo info : cluster) sumRetTime += info.retTime;
            aveRetTime = sumRetTime / cluster.size();
        }
        
        @Override
        public String toString() {
            return  "Cluster at " + DECIMAL.format(aveRetTime) + " min";
        }
    }
    
    
    private JPanel pnlVisible;
    private JPanel pnlLabelsFields;
    private JPanel pnlTabs;
    private JCheckBox preview;
    private JComboBox <PeakList> comboPeakList;
    private DefaultComboBoxModel <ComboClustersItem> comboClustersModel;
    private JComboBox <ComboClustersItem> comboClusters;
    private SimpleScatterPlot retTimeMZPlot;
    private EICPlot retTimeIntensityPlot;
    
    public static final List <PeakInfo> PEAK_INFO = new ArrayList <> ();
    public static final Map <PeakInfo, NavigableMap <Double, Double>> 
            CHROMATOGRAMS = new HashMap <> ();
    
    private Object[] currentValues;
    
    private List <List <PeakInfo>> retTimeClusters = null;
    
    public ADAP3DecompositionV1_5SetupDialog(
            Window parent,
            boolean valueCheckRequired,
            final ParameterSet parameters)
    {    
        super(parent, valueCheckRequired, parameters);
        
        Parameter[] params = parameters.getParameters();
        int size = params.length;
        
        currentValues = new Object[size];
        
        for (int i = 0; i < size; ++i) currentValues[i] = params[i].getValue();
    }
    
    @Override
    protected void addDialogComponents()
    {
        super.addDialogComponents();
        
        comboPeakList = new JComboBox <> ();
        comboClustersModel = new DefaultComboBoxModel <> ();
        comboClusters = new JComboBox <> (comboClustersModel);
        retTimeMZPlot = new SimpleScatterPlot("Retention time", "m/z");
        retTimeIntensityPlot = new EICPlot();
        
        PeakList[] peakLists = MZmineCore.getDesktop().getSelectedPeakLists();
        
        if (peakLists == null || peakLists.length == 0) return;
        
        // -----------------------------
        // Panel with preview parameters
        // -----------------------------
        
        preview = new JCheckBox("Show preview");
        preview.addActionListener(this);
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setEnabled(true);
        
        final JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.add(new JSeparator(), BorderLayout.NORTH);
        previewPanel.add(preview, BorderLayout.CENTER);
        previewPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
        
        comboPeakList.setFont(COMBO_FONT);
        for (final PeakList peakList : peakLists)
            if (peakList.getNumberOfRawDataFiles() == 1)
                comboPeakList.addItem(peakList);
        comboPeakList.addActionListener(this);
        comboPeakList.setSelectedIndex(0);
       
        comboClusters.setFont(COMBO_FONT);
        comboClusters.addActionListener(this);
        
        pnlLabelsFields = GUIUtils.makeTablePanel(2, 2, 
                new JComponent[] {new JLabel("Peak list"), comboPeakList,
                        new JLabel("Cluster list"), comboClusters});
        
        pnlVisible = new JPanel(new BorderLayout());
        pnlVisible.add(previewPanel, BorderLayout.NORTH);
        
        // --------------------------------------------------------------------
        // ----- Tabbed panel with plots --------------------------------------
        // --------------------------------------------------------------------
        
//        pnlTabs = new JTabbedPane();
        pnlTabs = new JPanel();
        pnlTabs.setLayout(new BoxLayout(pnlTabs, BoxLayout.Y_AXIS));
        
        ADAP3DecompositionV1_5Task.getInfoAndRawData(
                peakLists[0], PEAK_INFO, CHROMATOGRAMS);
        
        retTimeMZPlot.setMinimumSize(MIN_DIMENSIONS);
        
        JPanel pnlPlotRetTimeClusters = new JPanel(new BorderLayout());
        pnlPlotRetTimeClusters.setBackground(Color.white);
        pnlPlotRetTimeClusters.add(retTimeMZPlot, BorderLayout.CENTER);
        GUIUtils.addMarginAndBorder(pnlPlotRetTimeClusters, 10);
        
        pnlTabs.add(pnlPlotRetTimeClusters);
        
        retTimeIntensityPlot.setMinimumSize(MIN_DIMENSIONS);
        
        JPanel pnlPlotShapeClusters = new JPanel(new BorderLayout());
        pnlPlotShapeClusters.setBackground(Color.white);
        pnlPlotShapeClusters.add(retTimeIntensityPlot, BorderLayout.CENTER);
        GUIUtils.addMarginAndBorder(pnlPlotShapeClusters, 10);
        
        pnlTabs.add(pnlPlotShapeClusters);
        
        super.mainPanel.add(pnlVisible, 0, super.getNumberOfParameters() + 3, 
                2, 1, 0, 0, GridBagConstraints.HORIZONTAL);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);
        
        final Object source = e.getSource();

        if (source.equals(preview))
        {
            if (preview.isSelected()) {
                // Set the height of the preview to 200 cells, so it will span
                // the whole vertical length of the dialog (buttons are at row
                // no 100). Also, we set the weight to 10, so the preview
                // component will consume most of the extra available space.
                mainPanel.add(pnlTabs, 3, 0, 1, 200, 10, 10, 
                        GridBagConstraints.BOTH);
                pnlVisible.add(pnlLabelsFields, BorderLayout.CENTER);
            }
            else {
                mainPanel.remove(pnlTabs);
                pnlVisible.remove(pnlLabelsFields);
            }

            updateMinimumSize();
            pack();
            setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
        }

        else if (source.equals(comboPeakList)) 
        {
            // -------------------------
            // Retrieve current PeakList
            // -------------------------

            final PeakList peakList = (PeakList) comboPeakList.getSelectedItem();

            final List <PeakInfo> peakInfo = new ArrayList <> ();
            final Map <PeakInfo, NavigableMap <Double, Double>> chromatograms =
                    new HashMap <> ();

            ADAP3DecompositionV1_5Task
                    .getInfoAndRawData(peakList, peakInfo, chromatograms);

            // ---------------------------------
            // Calculate retention time clusters
            // ---------------------------------

            List <Double> retTimeValues = new ArrayList <> ();
            List <Double> mzValues = new ArrayList <> ();
            List <Double> colorValues = new ArrayList <> ();

            retTimeCluster(peakInfo, chromatograms, 
                    retTimeValues, mzValues, colorValues);

            final int size = retTimeValues.size();

            retTimeMZPlot.updateData(
                    ArrayUtils.toPrimitive(retTimeValues.toArray(new Double[size])),
                    ArrayUtils.toPrimitive(mzValues.toArray(new Double[size])),
                    ArrayUtils.toPrimitive(colorValues.toArray(new Double[size])));

            // ------------------------
            // Calculate shape clusters
            // ------------------------

            final ComboClustersItem item = 
                (ComboClustersItem) comboClusters.getSelectedItem();

            if (item != null)
            {
                final List <List <NavigableMap <Double, Double>>> shapeClusters = 
                        new ArrayList <> ();
                final List <List <String>> texts = new ArrayList <> ();
                final List <Double> colors = new ArrayList <> ();

                shapeCluster(item.cluster, chromatograms, 
                        shapeClusters, texts, colors);

                retTimeIntensityPlot.updateData(shapeClusters, colors, texts, null);
            }
        }

        else if (source.equals(comboClusters))
        {
            // ------------------------
            // Calculate shape clusters
            // ------------------------

            final ComboClustersItem item = 
                (ComboClustersItem) comboClusters.getSelectedItem();

            if (item != null)
            {
                final List <List <NavigableMap <Double, Double>>> shapeClusters = 
                        new ArrayList <> ();
                final List <List <String>> texts = new ArrayList <> ();
                final List <Double> colors = new ArrayList <> ();

                shapeCluster(item.cluster, item.chromatograms, 
                        shapeClusters, texts, colors);

                retTimeIntensityPlot.updateData(shapeClusters, colors, texts, null);
            }
        }
    }
    
    
    @Override
    public void parametersChanged()
    {
        super.updateParameterSetFromComponents();

        switch (compareParameters(parameterSet.getParameters()))
        {
            case FIRST_PHASE_CHANGE:
                comboPeakList.setSelectedIndex(comboPeakList.getSelectedIndex());
                break;
            
            case SECOND_PHASE_CHANGE:
                comboClusters.setSelectedIndex(comboClusters.getSelectedIndex());
                break;
        }
        
//        comboPeakList.setSelectedIndex(comboPeakList.getSelectedIndex());
//        // -------------------------
//        // Retrieve current PeakList
//        // -------------------------
//
//        final PeakList peakList = (PeakList) comboPeakList.getSelectedItem();
//
//        final List <PeakInfo> peakInfo = new ArrayList <> ();
//        final Map <PeakInfo, NavigableMap <Double, Double>> chromatograms =
//                new HashMap <> ();
//
//        ADAP3DecompositionV1_5Task
//                .getInfoAndRawData(peakList, peakInfo, chromatograms);
//
//        // ---------------------------------
//        // Calculate retention time clusters
//        // ---------------------------------
//
//        List <Double> retTimeValues = new ArrayList <> ();
//        List <Double> mzValues = new ArrayList <> ();
//        List <Double> colorValues = new ArrayList <> ();
//
//        retTimeCluster(peakInfo, chromatograms, 
//                retTimeValues, mzValues, colorValues);
//
//        final int size = retTimeValues.size();
//
//        retTimeMZPlot.updateData(
//                ArrayUtils.toPrimitive(retTimeValues.toArray(new Double[size])),
//                ArrayUtils.toPrimitive(mzValues.toArray(new Double[size])),
//                ArrayUtils.toPrimitive(colorValues.toArray(new Double[size])));
//
//        // ------------------------
//        // Calculate shape clusters
//        // ------------------------
//
//        final ComboClustersItem item = 
//            (ComboClustersItem) comboClusters.getSelectedItem();
//
//        final List <List <NavigableMap <Double, Double>>> shapeClusters = 
//                new ArrayList <> ();
//        final List <Double> colors = new ArrayList <> ();
//
//        shapeCluster(item.cluster, chromatograms, shapeClusters, colors);
//
//        retTimeIntensityPlot.updateData(shapeClusters, colors, null);
    }
    
    /**
     * Cluster all peaks in PeakList based on retention time
     * @param peakList PeakList object
     * @param retTimeValues output of retention times
     * @param mzValues output of m/z-values
     * @param colorValues output of colors
     */
    
    private void retTimeCluster(List <PeakInfo> peakInfo,
            Map <PeakInfo, NavigableMap <Double, Double>> chromatograms,
            List <Double> retTimeValues, 
            List <Double> mzValues, 
            List <Double> colorValues)
    {   
        Double minDistance = parameterSet.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_DISTANCE).getValue();
        Integer minSize = parameterSet.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_SIZE).getValue();
        Double minIntensity = parameterSet.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_INTENSITY).getValue();
        
        if (minDistance == null || minSize == null || minIntensity == null)
            return;
        
        retTimeClusters = TwoStepDecomposition.getRetTimeClusters(
                peakInfo, minDistance, minSize, minIntensity);
        
        int colorIndex = 0;
        final int numColors = 10;
        final double[] colors = new double[numColors];
        for (int i = 0; i < numColors; ++i) colors[i] = (double) i / numColors;
        
        comboClustersModel.removeAllElements();
        
        // Disable action listeners
        ActionListener[] comboListeners = comboClusters.getActionListeners();
        for (ActionListener l : comboListeners) 
            comboClusters.removeActionListener(l);
        
        for (List <PeakInfo> cluster : retTimeClusters)
        {
            for (PeakInfo info : cluster) {
                retTimeValues.add(info.retTime);
                mzValues.add(info.mzValue);
                colorValues.add(colors[colorIndex % numColors]);
            }
            
            ++colorIndex;
            
            ComboClustersItem newItem = new ComboClustersItem(cluster, chromatograms);
            
            int i;
            
            for (i = 0; i < comboClustersModel.getSize(); ++i)
            {
                double retTime = comboClustersModel.getElementAt(i).aveRetTime;
                if (newItem.aveRetTime < retTime) {
                    comboClustersModel.insertElementAt(newItem, i);
                    break;
                }
            }
            
            if (i == comboClustersModel.getSize())
                comboClustersModel.addElement(newItem);
        }
        
        // Enable action listeners
        for (ActionListener l : comboListeners) 
            comboClusters.addActionListener(l);
        
//        comboClusters.setSelectedIndex(0);
    }
    
    /**
     * Cluster list of PeakInfo based on the chromatographic shapes
     * 
     * @param peakInfo list of PeakInfo
     * @param chromatograms map of chromatograms
     * @param outClusters output of clusters
     * @param outColors output of colors
     */
    
    private void shapeCluster(List <PeakInfo> peakInfo,
            Map <PeakInfo, NavigableMap <Double, Double>> chromatograms,
            List <List <NavigableMap <Double, Double>>> outClusters,
            List <List <String>> outText,
            List <Double> outColors)
    {
        Double shapeSimThreshold = parameterSet.getParameter(
                ADAP3DecompositionV1_5Parameters.SHAPE_SIM_THRESHOLD).getValue();
        Double minModelPeakStN = parameterSet.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_MODEL_STN).getValue();
        Double minModelPeakSharpness = parameterSet.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_MODEL_SHARPNESS).getValue();
        List <Range <Double>> deprecatedMZValues = parameterSet.getParameter(
                ADAP3DecompositionV1_5Parameters.MZ_VALUES).getValue();
        
        if (shapeSimThreshold == null || minModelPeakStN == null 
                || minModelPeakSharpness == null 
                || deprecatedMZValues == null) return;
        
        List <PeakInfo> modelPeakCandidates = 
                TwoStepDecomposition.filterPeakInfo(peakInfo,
                        minModelPeakStN, minModelPeakSharpness,
                        deprecatedMZValues);
        
        if (modelPeakCandidates.isEmpty()) return;
        
        List <List <PeakInfo>> clusters = TwoStepDecomposition.getShapeClusters(
                    modelPeakCandidates, chromatograms, shapeSimThreshold);
        
        outClusters.clear();
        outText.clear();
        outColors.clear();
        
        Random rand = new Random();
        rand.setSeed(0);
        
        int colorIndex = 0;
        final int numColors = 10;
        final double[] colors = new double[numColors];
        
        for (int i = 0; i < numColors; ++i) colors[i] = rand.nextDouble();
        
        for (List <PeakInfo> cluster : clusters) 
        {
            List <NavigableMap <Double, Double>> c = 
                    new ArrayList <> (cluster.size());
            
            List <String> texts = new ArrayList <> (cluster.size());
            
            for (PeakInfo info : cluster) {
                c.add(chromatograms.get(info));
                texts.add(info.toString());
            }
            outClusters.add(c);
            outText.add(texts);
            
            outColors.add(colors[colorIndex % numColors]);
            ++colorIndex;
        }
    }
    
    private byte compareParameters(Parameter[] newValues)
    {
        if (currentValues == null) 
        {
            int size = newValues.length;
            currentValues = new Object[size];
            for (int i = 0; i < size; ++i) 
                currentValues[i] = newValues[i].getValue();
            
            return FIRST_PHASE_CHANGE;
        }
        
        final Set <Integer> firstPhaseIndices = 
                new HashSet <> (Arrays.asList(new Integer[] {1, 2, 3}));
        
        final Set <Integer> secondPhaseIndices = 
                new HashSet <> (Arrays.asList(new Integer[] {4, 5, 6, 8}));
        
        int size = Integer.min(currentValues.length, newValues.length);
        
        Set <Integer> changedIndices = new HashSet <> ();
        
        for (int i = 0; i < size; ++i) {
            Object oldValue = currentValues[i];
            Object newValue = newValues[i].getValue();
            if (newValue != null && oldValue != null 
                    && !oldValue.equals(newValue)) changedIndices.add(i);
        }
        
        byte result = NO_CHANGE;
        
        if (!Sets.intersection(firstPhaseIndices, changedIndices).isEmpty()) 
            result = FIRST_PHASE_CHANGE;
        
        else if (!Sets.intersection(secondPhaseIndices, changedIndices).isEmpty())
            result = SECOND_PHASE_CHANGE;
        
        for (int i = 0; i < size; ++i)
            currentValues[i] = newValues[i].getValue();
        
        return result;
    }
}
