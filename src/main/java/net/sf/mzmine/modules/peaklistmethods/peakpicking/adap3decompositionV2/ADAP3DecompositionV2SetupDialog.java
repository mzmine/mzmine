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
import dulab.adap.datamodel.BetterComponent;
import dulab.adap.datamodel.BetterPeak;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.swing.*;

import dulab.adap.workflow.decomposition.ComponentSelector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.GUIUtils;

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

    private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);

    /** One of three states:
     *  > no changes made,
     *  > change in the first phase parameters,
     *  > change in the second phase parameters
     */
    private enum CHANGE_STATE {NONE, FIRST_PHASE, SECOND_PHASE}

    /**
     * Elements of the interface
     */
    private JPanel pnlUIElements;
    private JPanel pnlComboBoxes;
    private JPanel pnlPlots;
    private JCheckBox chkPreview;
    private JComboBox<ChromatogramPeakPair> cboPeakLists;
    private JComboBox<RetTimeClusterer.Cluster> cboClusters;
    private JButton btnRefresh;
    private SimpleScatterPlot retTimeMZPlot;
    private EICPlot retTimeIntensityPlot;

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

//        PeakList[] peakLists = parameters.getParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS)
//                .getValue().getMatchingPeakLists();

//        if (peakLists.length == 0)
//            throw new IllegalArgumentException("At least one peak list has to be chosen");

//        if (peakLists.length > 0)
//            chromatograms = new ADAP3DecompositionV2Utils().getPeaks(peakLists[0]);
//        else
//            chromatograms = new ArrayList<>(0);
    }

    /** Creates the interface elements */
    @Override
    protected void addDialogComponents()
    {
        super.addDialogComponents();
        
        // -----------------------------
        // Panel with preview UI elements
        // -----------------------------

        // Preview CheckBox
        chkPreview = new JCheckBox("Show preview");
        chkPreview.addActionListener(this);
        chkPreview.setHorizontalAlignment(SwingConstants.CENTER);
        chkPreview.setEnabled(true);

        // Preview panel that will contain ComboBoxes
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JSeparator(), BorderLayout.NORTH);
        panel.add(chkPreview, BorderLayout.CENTER);
        panel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
        pnlUIElements = new JPanel(new BorderLayout());
        pnlUIElements.add(panel, BorderLayout.NORTH);

        // ComboBox for Peak lists
        cboPeakLists = new JComboBox<>();
        cboPeakLists.setFont(COMBO_FONT);
        for (ChromatogramPeakPair p : ChromatogramPeakPair.fromParameterSet(parameterSet).values())
            cboPeakLists.addItem(p);
        cboPeakLists.addActionListener(this);

        java.net.URL refreshImageURL = this.getClass().getResource("images/refresh.16.png");
        btnRefresh = refreshImageURL != null ?
                new JButton(new ImageIcon(refreshImageURL)) :
                new JButton("Refresh");
        btnRefresh.addActionListener(this);

        // ComboBox with Clusters
        cboClusters = new JComboBox<>();
        cboClusters.setFont(COMBO_FONT);
        cboClusters.addActionListener(this);

        pnlComboBoxes = GUIUtils.makeTablePanel(2, 3, 1, new JComponent[] {
                new JLabel("Peak Lists"), cboPeakLists, btnRefresh,
                new JLabel("Clusters"), cboClusters, new JPanel()});
        
        // --------------------------------------------------------------------
        // ----- Panel with plots --------------------------------------
        // --------------------------------------------------------------------

        pnlPlots = new JPanel();
        pnlPlots.setLayout(new BoxLayout(pnlPlots, BoxLayout.Y_AXIS));

        // Plot with retention-time clusters
        retTimeMZPlot = new SimpleScatterPlot("Retention time", "m/z");
        retTimeMZPlot.setMinimumSize(MIN_DIMENSIONS);
        retTimeMZPlot.setPreferredSize(MIN_DIMENSIONS);
        
        final JPanel pnlPlotRetTimeClusters = new JPanel(new BorderLayout());
        pnlPlotRetTimeClusters.setBackground(Color.white);
        pnlPlotRetTimeClusters.add(retTimeMZPlot, BorderLayout.CENTER);
        GUIUtils.addMarginAndBorder(pnlPlotRetTimeClusters, 10);

        // Plot with chromatograms
        retTimeIntensityPlot = new EICPlot();
        retTimeIntensityPlot.setMinimumSize(MIN_DIMENSIONS);
        retTimeIntensityPlot.setPreferredSize(MIN_DIMENSIONS);
        
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

                refresh();
            }
            else {
                mainPanel.remove(pnlPlots);
                pnlUIElements.remove(pnlComboBoxes);
            }

            updateMinimumSize();
            pack();
            setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
        }

        else  if (source.equals(btnRefresh))
            refresh();

        else if (source.equals(cboClusters))
            shapeCluster();
    }
    
    
    @Override
    public void parametersChanged()
    {
        super.updateParameterSetFromComponents();

        if (!chkPreview.isSelected()) return;

        Cursor cursor = this.getCursor();
        this.setCursor(WAIT_CURSOR);

        switch (compareParameters(parameterSet.getParameters()))
        {
            case FIRST_PHASE:
                retTimeCluster();
                break;
            
            case SECOND_PHASE:
                shapeCluster();
                break;
        }

        this.setCursor(cursor);
    }

    private void refresh()
    {
        cboPeakLists.removeActionListener(this);
        cboPeakLists.removeAllItems();
        for (ChromatogramPeakPair p : ChromatogramPeakPair.fromParameterSet(parameterSet).values())
            cboPeakLists.addItem(p);
        cboPeakLists.addActionListener(this);
        if (cboPeakLists.getItemCount() > 0)
            cboPeakLists.setSelectedIndex(0);

        retTimeCluster();
    }

    /**
     * Cluster all peaks in PeakList based on retention time
     */
    private void retTimeCluster()
    {
        ChromatogramPeakPair chromatogramPeakPair = cboPeakLists.getItemAt(cboPeakLists.getSelectedIndex());
        if (chromatogramPeakPair == null) return;

        PeakList chromatogramList = chromatogramPeakPair.chromatograms;
        PeakList peakList = chromatogramPeakPair.peaks;
        if (chromatogramList == null || peakList == null) return;

        Double minDistance = parameterSet.getParameter(ADAP3DecompositionV2Parameters.PREF_WINDOW_WIDTH).getValue();
        Integer minSize = parameterSet.getParameter(ADAP3DecompositionV2Parameters.MIN_NUM_PEAK).getValue();
        if (minDistance == null || minSize == null) return;

        // Convert peakList into ranges
        RetTimeClusterer.Item[] ranges = Arrays.stream(peakList.getRows())
                .map(PeakListRow::getBestPeak)
                .map(p -> new RetTimeClusterer.Item(p.getRT(), p.getRawDataPointsRTRange(), p.getMZ()))
                .toArray(RetTimeClusterer.Item[]::new);

        // Form clusters of ranges
        List<RetTimeClusterer.Cluster> retTimeClusters = new RetTimeClusterer(minDistance, minSize).execute(ranges);

//        int colorIndex = 0;
//        final int numColors = 7;
//        final double[] colors = new double[numColors];
//        for (int i = 0; i < numColors; ++i) colors[i] = (double) i / numColors;

        cboClusters.removeAllItems();
        cboClusters.removeActionListener(this);

//        List <Double> retTimeValues = new ArrayList <> ();
//        List <Double> mzValues = new ArrayList <> ();
//        List <Double> colorValues = new ArrayList <> ();

        for (RetTimeClusterer.Cluster cluster : retTimeClusters)
        {
//            for (RetTimeClusterer.Item range : cluster.ranges) {
//                retTimeValues.add(range.getValue());
//                mzValues.add(range.getMZ());
//                colorValues.add(colors[colorIndex % numColors]);
//            }

//            ++colorIndex;

            int i;

            for (i = 0; i < cboClusters.getItemCount(); ++i)
            {
                double retTime = cboClusters.getItemAt(i).retTime;
                if (cluster.retTime < retTime) {
                    cboClusters.insertItemAt(cluster, i);
                    break;
                }
            }

            if (i == cboClusters.getItemCount())
                cboClusters.addItem(cluster);
        }

        cboClusters.addActionListener(this);

        retTimeMZPlot.updateData(retTimeClusters);

        shapeCluster();
    }
    
    /**
     * Cluster list of PeakInfo based on the chromatographic shapes
     */
    private void shapeCluster()
    {
        ChromatogramPeakPair chromatogramPeakPair = cboPeakLists.getItemAt(cboPeakLists.getSelectedIndex());
        if (chromatogramPeakPair == null) return;

        PeakList chromatogramList = chromatogramPeakPair.chromatograms;
        PeakList peakList = chromatogramPeakPair.peaks;
        if (chromatogramList == null || peakList == null) return;

        final RetTimeClusterer.Cluster cluster = cboClusters.getItemAt(cboClusters.getSelectedIndex());
        if (cluster == null) return;

        Double retTimeTolerance = parameterSet.getParameter(
                ADAP3DecompositionV2Parameters.RET_TIME_TOLERANCE).getValue();
        if (retTimeTolerance == null || retTimeTolerance <= 0.0)
            return;

        List<BetterPeak> chromatograms = new ADAP3DecompositionV2Utils().getPeaks(chromatogramList);

        List<BetterComponent> components = null;
        try {
            components = new ComponentSelector(cluster, chromatograms, retTimeTolerance).run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Set<Double> mzSet = cluster.ranges.stream()
                .map(RetTimeClusterer.Item::getMZ).collect(Collectors.toSet());

        chromatograms = chromatograms.stream()
                .filter(c -> mzSet.contains(c.mzValue)).collect(Collectors.toList());

        if (components != null)
            retTimeIntensityPlot.updateData(chromatograms, components);
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
        
        final Set <Integer> firstPhaseIndices = new HashSet <> (Arrays.asList(2, 3));
        final Set <Integer> secondPhaseIndices = new HashSet<>(Collections.singletonList(4));
        
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
