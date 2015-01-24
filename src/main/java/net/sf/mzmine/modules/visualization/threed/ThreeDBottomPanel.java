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

package net.sf.mzmine.modules.visualization.threed;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

/**
 * 3D visualizer's bottom panel
 */
class ThreeDBottomPanel extends JPanel implements TreeModelListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JComboBox<PeakList> peakListSelector;
    private JCheckBox showIdChkBox;

    private RawDataFile dataFile;

    ThreeDBottomPanel(ThreeDVisualizerWindow masterFrame, RawDataFile dataFile) {

	this.dataFile = dataFile;

	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

	setBackground(Color.white);
	setBorder(new EmptyBorder(5, 5, 5, 0));

	add(Box.createHorizontalGlue());

	GUIUtils.addLabel(this, "Peak list: ", SwingConstants.RIGHT);

	peakListSelector = new JComboBox<PeakList>();
	peakListSelector.setBackground(Color.white);
	peakListSelector.setFont(smallFont);
	peakListSelector.addActionListener(masterFrame);
	peakListSelector.setActionCommand("PEAKLIST_CHANGE");
	add(peakListSelector);

	add(Box.createHorizontalStrut(10));

	showIdChkBox = new JCheckBox("Show compound name");
	showIdChkBox.setSelected(true);
	showIdChkBox.setBackground(Color.white);
	showIdChkBox.setFont(smallFont);
	showIdChkBox.addActionListener(masterFrame);
	showIdChkBox.setActionCommand("PEAKLIST_CHANGE");
	// TODO Improve functionality of this button, currently it's behavior is
	// confusing with "Show peaks value" button
	// add(showIdChkBox);

	add(Box.createHorizontalGlue());

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

	logger.finest("Rebuilding the peak list selector");

	PeakList selectedPeakList = (PeakList) peakListSelector
		.getSelectedItem();
	PeakList currentPeakLists[] = MZmineCore.getProjectManager()
		.getCurrentProject().getPeakLists(dataFile);
	peakListSelector.removeAllItems();
	for (int i = currentPeakLists.length - 1; i >= 0; i--) {
	    peakListSelector.addItem(currentPeakLists[i]);
	}
	if (selectedPeakList != null)
	    peakListSelector.setSelectedItem(selectedPeakList);
    }

    boolean showCompoundNameSelected() {
	return showIdChkBox.isSelected();
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
