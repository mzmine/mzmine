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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.threed;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectListener;
import net.sf.mzmine.util.GUIUtils;

/**
 * 3D visualizer's bottom panel
 */
class ThreeDBottomPanel extends JPanel implements ProjectListener,
        InternalFrameListener {

    private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JComboBox peakListSelector;
    private JCheckBox showIdChkBox;

    private ThreeDVisualizerWindow masterFrame;
    private RawDataFile dataFile;

    ThreeDBottomPanel(ThreeDVisualizerWindow masterFrame, RawDataFile dataFile) {

        this.dataFile = dataFile;
        this.masterFrame = masterFrame;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        setBackground(Color.white);
        setBorder(new EmptyBorder(5, 5, 5, 0));

        add(Box.createHorizontalGlue());

        GUIUtils.addLabel(this, "Peak list: ", SwingConstants.RIGHT);

        peakListSelector = new JComboBox();
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
        add(showIdChkBox);

        MZmineCore.getProjectManager().addProjectListener(this);

        masterFrame.addInternalFrameListener(this);

        add(Box.createHorizontalGlue());

    }

    /**
     * Returns selected peak list
     */
    PeakList getSelectedPeakList() {
        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        return selectedPeakList;
    }

    /**
     * Reloads peak lists from the project to the selector combo box
     */
    void rebuildPeakListSelector() {
        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        PeakList currentPeakLists[] = MZmineCore.getCurrentProject().getPeakLists(
                dataFile);
        peakListSelector.removeAllItems();
        for (int i = currentPeakLists.length - 1; i >= 0; i--) {
            peakListSelector.addItem(currentPeakLists[i]);
        }
        if (selectedPeakList != null)
            peakListSelector.setSelectedItem(selectedPeakList);
    }

    /**
     * ProjectListener implementaion
     */
    public void projectModified(ProjectEvent event) {
        rebuildPeakListSelector();
    }

    public void internalFrameActivated(InternalFrameEvent event) {
        // Ignore
    }

    /**
     * We have to remove the listener when the window is closed, because
     * otherwise the project would always keep a reference to this window and
     * the GC would not be able to collect it
     */
    public void internalFrameClosed(InternalFrameEvent event) {
        MZmineCore.getProjectManager().removeProjectListener(this);
        masterFrame.removeInternalFrameListener(this);
    }

    public void internalFrameClosing(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameDeactivated(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameDeiconified(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameIconified(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameOpened(InternalFrameEvent event) {
        // Ignore
    }

    boolean showCompoundNameSelected() {
        return showIdChkBox.isSelected();
    }
}
