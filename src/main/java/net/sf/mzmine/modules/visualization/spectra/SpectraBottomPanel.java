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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

/**
 * Spectra visualizer's bottom panel
 */
class SpectraBottomPanel extends JPanel implements TreeModelListener {

    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // Get arrow characters by their UTF16 code
    public static final String leftArrow = new String(new char[] { '\u2190' });
    public static final String rightArrow = new String(new char[] { '\u2192' });

    public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JPanel topPanel, bottomPanel;
    private JComboBox<String> msmsSelector;
    private JComboBox<PeakList> peakListSelector;

    private RawDataFile dataFile;
    private SpectraVisualizerWindow masterFrame;

    // Last time the data set was redrawn.
    private static long lastRebuildTime = System.currentTimeMillis();

    // Refresh interval (in milliseconds).
    private static final long REDRAW_INTERVAL = 1000L;

    SpectraBottomPanel(SpectraVisualizerWindow masterFrame,
            RawDataFile dataFile) {

        super(new BorderLayout());
        this.dataFile = dataFile;
        this.masterFrame = masterFrame;

        setBackground(Color.white);

        topPanel = new JPanel();
        topPanel.setBackground(Color.white);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        add(topPanel, BorderLayout.CENTER);

        topPanel.add(Box.createHorizontalStrut(10));

        JButton prevScanBtn = GUIUtils.addButton(topPanel, leftArrow, null,
                masterFrame, "PREVIOUS_SCAN");
        prevScanBtn.setBackground(Color.white);
        prevScanBtn.setFont(smallFont);

        topPanel.add(Box.createHorizontalGlue());

        GUIUtils.addLabel(topPanel, "Peak list: ", SwingConstants.RIGHT);

        peakListSelector = new JComboBox<PeakList>();
        peakListSelector.setBackground(Color.white);
        peakListSelector.setFont(smallFont);
        peakListSelector.addActionListener(masterFrame);
        peakListSelector.setActionCommand("PEAKLIST_CHANGE");
        topPanel.add(peakListSelector);

        topPanel.add(Box.createHorizontalGlue());

        JButton nextScanBtn = GUIUtils.addButton(topPanel, rightArrow, null,
                masterFrame, "NEXT_SCAN");
        nextScanBtn.setBackground(Color.white);
        nextScanBtn.setFont(smallFont);

        topPanel.add(Box.createHorizontalStrut(10));

        bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.white);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        add(bottomPanel, BorderLayout.SOUTH);

        bottomPanel.add(Box.createHorizontalGlue());

        GUIUtils.addLabel(bottomPanel, "MS/MS: ", SwingConstants.RIGHT);

        msmsSelector = new JComboBox<String>();
        msmsSelector.setBackground(Color.white);
        msmsSelector.setFont(smallFont);
        bottomPanel.add(msmsSelector);

        JButton showButton = GUIUtils.addButton(bottomPanel, "Show", null,
                masterFrame, "SHOW_MSMS");
        showButton.setBackground(Color.white);
        showButton.setFont(smallFont);

        bottomPanel.add(Box.createHorizontalGlue());

    }

    JComboBox<String> getMSMSSelector() {
        return msmsSelector;
    }

    void setMSMSSelectorVisible(boolean visible) {
        bottomPanel.setVisible(visible);
    }

    /**
     * Returns selected peak list
     */
    PeakList getSelectedPeakList() {
        PeakList selectedPeakList = (PeakList) peakListSelector
                .getSelectedItem();
        return selectedPeakList;
    }

    /**
     * Reloads peak lists from the project to the selector combo box
     */
    void rebuildPeakListSelector() {

        // Refresh every REDRAW_INTERVAL ms.
        if (System.currentTimeMillis() - lastRebuildTime < REDRAW_INTERVAL)
            return;

        logger.finest("Rebuilding the peak list selector");

        PeakList selectedPeakList = (PeakList) peakListSelector
                .getSelectedItem();
        PeakList currentPeakLists[] = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists(dataFile);
        peakListSelector.setEnabled(false);
        peakListSelector.removeActionListener(masterFrame);
        peakListSelector.removeAllItems();

        // Add all peak lists in reverse order (last added peak list will be
        // first)
        for (int i = currentPeakLists.length - 1; i >= 0; i--) {
            peakListSelector.addItem(currentPeakLists[i]);
        }

        // If there is any peak list, make a selection
        if (currentPeakLists.length > 0) {
            peakListSelector.setEnabled(true);
            peakListSelector.addActionListener(masterFrame);
            if (selectedPeakList != null)
                peakListSelector.setSelectedItem(selectedPeakList);
            else
                peakListSelector.setSelectedIndex(0);
        }

        // Update last rebuild time
        lastRebuildTime = System.currentTimeMillis();

    }

    @Override
    public void treeNodesChanged(TreeModelEvent event) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
                .getTreePath().getLastPathComponent();
        if (node.getUserObject() instanceof PeakList)
            rebuildPeakListSelector();
    }

    @Override
    public void treeNodesInserted(TreeModelEvent event) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
                .getTreePath().getLastPathComponent();
        if (node.getUserObject() instanceof PeakList)
            rebuildPeakListSelector();
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent event) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
                .getTreePath().getLastPathComponent();
        if (node.getUserObject() instanceof PeakList)
            rebuildPeakListSelector();
    }

    @Override
    public void treeStructureChanged(TreeModelEvent event) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
                .getTreePath().getLastPathComponent();
        if (node.getUserObject() instanceof PeakList)
            rebuildPeakListSelector();
    }

}
