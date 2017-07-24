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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import com.google.common.collect.Sets;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import javax.annotation.Nonnull;
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

import dulab.adap.workflow.decomposition.Decomposition;
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


public class ADAP3DecompositionV2SetupDialog extends ParameterSetupDialog
{
    /** Minimum dimensions of plots */
    private static final Dimension MIN_DIMENSIONS = new Dimension(400, 300);

    /** Font for the preview combo elements */
    private static final Font COMBO_FONT = new Font("SansSerif", Font.PLAIN,10);

    /** One of three states:
     *  > no changes made,
     *  > change in the first phase parameters,
     *  > change in the second phase parameters
     */
    private enum CHANGE_STATE {NONE, FIRST_PHASE, SECOND_PHASE};

//    private static final byte NO_CHANGE = 0;
//    private static final byte FIRST_PHASE_CHANGE = 1;
//    private static final byte SECOND_PHASE_CHANGE = 2;
    
    private static class ComboClustersItem {
        private static final DecimalFormat DECIMAL = new DecimalFormat("#.00");
        private final List <Peak> cluster;
        private final double aveRetTime;
        
        ComboClustersItem(List <Peak> cluster) {
            this.cluster = cluster;
            
            double sumRetTime = 0.0;
            for (Peak peak : cluster) sumRetTime += peak.getRetTime();
            aveRetTime = sumRetTime / cluster.size();
        }
        
        @Override
        public String toString() {
            return  "Cluster at " + DECIMAL.format(aveRetTime) + " min";
        }
    }

    /**
     * Elements of the interface
     */
    private JPanel pnlUIElements;
    private JPanel pnlComboBoxes;
    private JPanel pnlPlots;
    private JCheckBox chkPreview;
//    private JComboBox <PeakList> cboPeakLists;
    private DefaultComboBoxModel <ComboClustersItem> comboClustersModel;
    private JComboBox <ComboClustersItem> cboClusters;
    private SimpleScatterPlot retTimeMZPlot;
    private EICPlot retTimeIntensityPlot;

    private final List<Peak> peaks;

    /** Current values of the parameters */
    private Object[] currentParameters;

    /** Creates an instance of the class and saves the current values of all parameters */
    ADAP3DecompositionV2SetupDialog(Window parent, boolean valueCheckRequired,
            @Nonnull final ParameterSet parameters)
    {    
        super(parent, valueCheckRequired, parameters);
        
        Parameter[] params = parameters.getParameters();
        int size = params.length;
        
        currentParameters = new Object[size];
        for (int i = 0; i < size; ++i)
            currentParameters[i] = params[i].getValue();

        PeakList[] peakLists = parameters.getParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();
        if (peakLists.length != 1)
            throw new IllegalArgumentException("One peak list has to be chosen");

        peaks = ADAP3DecompositionV2Task.getPeaks(peakLists[0]);
    }

    /** Creates the interface elements */
    @Override
    protected void addDialogComponents()
    {
        super.addDialogComponents();
        

        comboClustersModel = new DefaultComboBoxModel <> ();

        
        PeakList[] peakLists = MZmineCore.getDesktop().getSelectedPeakLists();
        
        // -----------------------------
        // Panel with preview UI elements
        // -----------------------------

        // Preview CheckBox
        chkPreview = new JCheckBox("Show preview");
        chkPreview.addActionListener(this);
        chkPreview.setHorizontalAlignment(SwingConstants.CENTER);
        chkPreview.setEnabled(peakLists != null && peakLists.length > 0);

        // Preview panel that will contain ComboBoxes
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JSeparator(), BorderLayout.NORTH);
        panel.add(chkPreview, BorderLayout.CENTER);
        panel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
        pnlUIElements = new JPanel(new BorderLayout());
        pnlUIElements.add(panel, BorderLayout.NORTH);

        // ComboBox with Clusters
        cboClusters = new JComboBox <> (comboClustersModel);
        cboClusters.setFont(COMBO_FONT);
        cboClusters.addActionListener(this);

        pnlComboBoxes = GUIUtils.makeTablePanel(1, 2,
                new JComponent[] {new JLabel("Clusters"), cboClusters});
        
        // --------------------------------------------------------------------
        // ----- Panel with plots --------------------------------------
        // --------------------------------------------------------------------

        pnlPlots = new JPanel();
        pnlPlots.setLayout(new BoxLayout(pnlPlots, BoxLayout.Y_AXIS));

        // Plot with retention-time clusters
        retTimeMZPlot = new SimpleScatterPlot("Retention time", "m/z");
        retTimeMZPlot.setMinimumSize(MIN_DIMENSIONS);
        
        final JPanel pnlPlotRetTimeClusters = new JPanel(new BorderLayout());
        pnlPlotRetTimeClusters.setBackground(Color.white);
        pnlPlotRetTimeClusters.add(retTimeMZPlot, BorderLayout.CENTER);
        GUIUtils.addMarginAndBorder(pnlPlotRetTimeClusters, 10);

        // Plot with chromatograms
        retTimeIntensityPlot = new EICPlot();
        retTimeIntensityPlot.setMinimumSize(MIN_DIMENSIONS);
        
        JPanel pnlPlotShapeClusters = new JPanel(new BorderLayout());
        pnlPlotShapeClusters.setBackground(Color.white);
        pnlPlotShapeClusters.add(retTimeIntensityPlot, BorderLayout.CENTER);
        GUIUtils.addMarginAndBorder(pnlPlotShapeClusters, 10);

        pnlPlots.add(pnlPlotRetTimeClusters);
        pnlPlots.add(pnlPlotShapeClusters);
        
        super.mainPanel.add(pnlUIElements, 0, super.getNumberOfParameters() + 3,
                2, 1, 0, 0, GridBagConstraints.HORIZONTAL);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);
        
        final Object source = e.getSource();

        if (source.equals(chkPreview))
        {
            if (chkPreview.isSelected()) {
                // Set the height of the chkPreview to 200 cells, so it will span
                // the whole vertical length of the dialog (buttons are at row
                // no 100). Also, we set the weight to 10, so the chkPreview
                // component will consume most of the extra available space.
                mainPanel.add(pnlPlots, 3, 0, 1, 200, 10, 10,
                        GridBagConstraints.BOTH);
                pnlUIElements.add(pnlComboBoxes, BorderLayout.CENTER);
//                cboPeakLists.setSelectedIndex(0);
            }
            else {
                mainPanel.remove(pnlPlots);
                pnlUIElements.remove(pnlComboBoxes);
            }

            updateMinimumSize();
            pack();
            setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());

            retTimeCluster();
        }

        else if (source.equals(cboClusters))
            shapeCluster();
    }
    
    
    @Override
    public void parametersChanged()
    {
        super.updateParameterSetFromComponents();

        if (!chkPreview.isSelected()) return;
        
        switch (compareParameters(parameterSet.getParameters()))
        {
            case FIRST_PHASE:
                retTimeCluster();
                break;
            
            case SECOND_PHASE:
                shapeCluster();
                break;
        }
    }
    
    /**
     * Cluster all peaks in PeakList based on retention time
     */
    
    private void retTimeCluster()
    {
        List <Double> retTimeValues = new ArrayList <> ();
        List <Double> mzValues = new ArrayList <> ();
        List <Double> colorValues = new ArrayList <> ();


        Double minDistance = parameterSet.getParameter(
                ADAP3DecompositionV2Parameters.MIN_CLUSTER_DISTANCE).getValue();
        Integer minSize = parameterSet.getParameter(
                ADAP3DecompositionV2Parameters.MIN_CLUSTER_SIZE).getValue();
        
        if (minDistance == null || minSize == null) return;

        List<List<Peak>> retTimeClusters = Decomposition
                .getRetTimeClusters(peaks, minDistance, minSize);

        int colorIndex = 0;
        final int numColors = 7;
        final double[] colors = new double[numColors];
        for (int i = 0; i < numColors; ++i) colors[i] = (double) i / numColors;
        
        comboClustersModel.removeAllElements();
        
        // Disable action listeners
        ActionListener[] comboListeners = cboClusters.getActionListeners();
        for (ActionListener l : comboListeners) 
            cboClusters.removeActionListener(l);
        
        for (List <Peak> cluster : retTimeClusters)
        {
            for (Peak peak : cluster) {
                retTimeValues.add(peak.getRetTime());
                mzValues.add(peak.getMZ());
                colorValues.add(colors[colorIndex % numColors]);
            }
            
            ++colorIndex;
            
            ComboClustersItem newItem = new ComboClustersItem(cluster);
            
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
            cboClusters.addActionListener(l);

        final int size = retTimeValues.size();

        retTimeMZPlot.updateData(
                ArrayUtils.toPrimitive(retTimeValues.toArray(new Double[size])),
                ArrayUtils.toPrimitive(mzValues.toArray(new Double[size])),
                ArrayUtils.toPrimitive(colorValues.toArray(new Double[size])));

        shapeCluster();
    }
    
    /**
     * Cluster list of PeakInfo based on the chromatographic shapes
     */
    private void shapeCluster()
    {
        final ComboClustersItem item = (ComboClustersItem) cboClusters.getSelectedItem();

        if (item == null) return;

        List<Peak> peaks = item.cluster;

        final List <List <NavigableMap <Double, Double>>> outClusters = new ArrayList <> ();
        final List <List<Boolean>> outModels = new ArrayList<>();
        final List <List <String>> outTexts = new ArrayList <> ();
        final List <Double> outColors = new ArrayList <> ();

//        List<Peak> modelPeaks = shapeCluster(item.cluster, shapeClusters, models, texts, colors);

//                retTimeIntensityPlot.updateData(shapeClusters, colors, texts, models);


        NumberFormat numberFormat = NumberFormat.getNumberInstance();

        Double fwhmTolerance = parameterSet.getParameter(
                ADAP3DecompositionV2Parameters.FWHM_TOLERANCE).getValue();
        Double shapeTolerance = parameterSet.getParameter(
                ADAP3DecompositionV2Parameters.SHAPE_TOLERANCE).getValue();

        List<Component> clusters = null;
        try {
            clusters = Decomposition.getShapeClusters(peaks, fwhmTolerance, shapeTolerance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List <Peak> modelPeaks = new ArrayList<>(clusters.size());
        for (Component c : clusters)
            modelPeaks.add(c.getBestPeak());

        
        outClusters.clear();
        outModels.clear();
        outTexts.clear();
        outColors.clear();
        
        Random rand = new Random();
        rand.setSeed(0);
        
        int colorIndex = 0;
        final int numColors = 10;
        final double[] colors = new double[numColors];
        
        for (int i = 0; i < numColors; ++i) colors[i] = rand.nextDouble();
        
        for (Component component : clusters)
        {
            final int numPeaks = component.size();
            List <NavigableMap <Double, Double>> c = new ArrayList <> (numPeaks);
            List <Boolean> models = new ArrayList<>(numPeaks);
            List <String> texts = new ArrayList <> (numPeaks);
            
            for (Peak peak : component.getPeaks())
            {
                c.add(peak.getChromatogram());

                boolean isModel = peak == component.getBestPeak();
                models.add(isModel);

//                double error = 0.0;
//                try {
//                    error = Decomposition.getGaussianFitError(peak) / peak.getIntensity();
//                }
//                catch (IllegalArgumentException e) {}

                String text = "";
                if (isModel) text += "Model Peak\n";
                text += peak.getInfo();
//                text += "\nSharpness: " + numberFormat.format(FeatureTools.sharpnessYang(peak.getChromatogram()));
                text += "\nGFE: " + peak.getInfo().gaussianFitError;
                texts.add(text);
            }
            outClusters.add(c);
            outModels.add(models);
            outTexts.add(texts);
            
            outColors.add(colors[colorIndex % numColors]);
            ++colorIndex;
        }

        retTimeIntensityPlot.updateData(item.cluster, modelPeaks);

//        return modelPeaks;
    }
    
    private CHANGE_STATE compareParameters(Parameter[] newValues)
    {
        if (currentParameters == null)
        {
            int size = newValues.length;
            currentParameters = new Object[size];
            for (int i = 0; i < size; ++i) 
                currentParameters[i] = newValues[i].getValue();
            
            return CHANGE_STATE.FIRST_PHASE;
        }
        
        final Set <Integer> firstPhaseIndices = 
                new HashSet <> (Arrays.asList(new Integer[] {1, 2}));
        
        final Set <Integer> secondPhaseIndices = 
                new HashSet <> (Arrays.asList(new Integer[] {3, 4}));
        
        int size = Math.min(currentParameters.length, newValues.length);
        
        Set <Integer> changedIndices = new HashSet <> ();
        
        for (int i = 0; i < size; ++i) 
        {
            Object oldValue = currentParameters[i];
            Object newValue = newValues[i].getValue();
            
            if (newValue != null && oldValue != null 
                    && oldValue.equals(newValue)) continue;
            
            changedIndices.add(i);
        }
        
        CHANGE_STATE result = CHANGE_STATE.NONE;
        
        if (!Sets.intersection(firstPhaseIndices, changedIndices).isEmpty()) 
            result = CHANGE_STATE.FIRST_PHASE;
        
        else if (!Sets.intersection(secondPhaseIndices, changedIndices).isEmpty())
            result = CHANGE_STATE.SECOND_PHASE;
        
        for (int i = 0; i < size; ++i)
            currentParameters[i] = newValues[i].getValue();
        
        return result;
    }
}
