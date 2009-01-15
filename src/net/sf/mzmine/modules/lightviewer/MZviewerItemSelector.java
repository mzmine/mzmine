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

package net.sf.mzmine.modules.lightviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableWindow;
import net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotWindow;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.DragOrderedJList;
import net.sf.mzmine.util.dialogs.NameChangeDialog;
import net.sf.mzmine.util.dialogs.NameChangeable;

public class MZviewerItemSelector extends JPanel implements ActionListener,
        MouseListener, ListSelectionListener {

    public static final String PEAK_LISTS_LABEL = "Peak lists";

    private DragOrderedJList peakLists;
    private DefaultListModel peakListsModel;
    private JPopupMenu peakListPopupMenu;
    private Desktop desktop;

    /**
     * Constructor
     */
    public MZviewerItemSelector(Desktop desktop) {

    	this.desktop = desktop;
        peakListsModel = new DefaultListModel();
        peakLists = new DragOrderedJList(peakListsModel);
        peakLists.addMouseListener(this);
        peakLists.addListSelectionListener(this);
        JScrollPane resultScroll = new JScrollPane(peakLists);

        JPanel peakListPanel = new JPanel();
        peakListPanel.setLayout(new BorderLayout());
        
        JLabel resultsTitle = new JLabel(PEAK_LISTS_LABEL);
        peakListPanel.add(resultsTitle, BorderLayout.NORTH);

        peakListPanel.add(resultScroll, BorderLayout.CENTER);
        peakListPanel.setMinimumSize(new Dimension(200, 10));

        setPreferredSize(new Dimension(200, 10));
        setLayout(new BorderLayout());
        add(peakListPanel, BorderLayout.CENTER);


        peakListPopupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list", this,
                "SHOW_ALIGNED_PEAKLIST");
        GUIUtils.addMenuItem(peakListPopupMenu, "Show scatter plot", this,
        "SHOW_SCATTER_PLOT_PEAKLIST");
        GUIUtils.addMenuItem(peakListPopupMenu, "Rename", this,
                "RENAME_PEAKLIST");
        GUIUtils.addMenuItem(peakListPopupMenu, "Remove", this,
                "REMOVE_PEAKLIST");

    }

    void addSelectionListener(ListSelectionListener listener) {
        peakLists.addListSelectionListener(listener);
    }

    // Implementation of action listener interface

    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        if (command.equals("RENAME_PEAKLIST")) {
            PeakList[] selectedPeakLists = getSelectedPeakLists();
            for (PeakList peakList : selectedPeakLists) {
                if (peakList instanceof NameChangeable) {
                    NameChangeDialog dialog = new NameChangeDialog(
                            (NameChangeable) peakList);
                    dialog.setVisible(true);
                }
            }
        }

        if (command.equals("REMOVE_PEAKLIST")) {
   			int[] indexes = peakLists.getSelectedIndices();
   			for(int index: indexes)
   				peakListsModel.remove(index);
        }

        if (command.equals("SHOW_ALIGNED_PEAKLIST")) {
            PeakList[] selectedPeakLists = getSelectedPeakLists();
            for (PeakList peakList : selectedPeakLists) {
                PeakListTableWindow window = new PeakListTableWindow(peakList);
                desktop.addInternalFrame(window);
            }
        }

        if (command.equals("SHOW_SCATTER_PLOT_PEAKLIST")) {
            PeakList[] selectedPeakLists = getSelectedPeakLists();
            for (PeakList peakList : selectedPeakLists) {
                if (peakList.getNumberOfRawDataFiles()<2){
                    desktop.displayErrorMessage("There is only one raw data file in " + peakList.toString() + ". It is necessary at least two for comparison");
                    continue;
                }
                ScatterPlotWindow window = new ScatterPlotWindow(peakList, "Scatter plot of " + peakList.toString());
                desktop.addInternalFrame(window);
            }
        }

    }


    public PeakList[] getSelectedPeakLists() {

        Object o[] = peakLists.getSelectedValues();

        PeakList res[] = new PeakList[o.length];

        for (int i = 0; i < o.length; i++) {
            res[i] = (PeakList) (o[i]);
        }

        return res;

    }
    
    public void addPeakList(PeakList newPeakList){
    	peakListsModel.addElement(newPeakList);
    }

    public void mouseClicked(MouseEvent e) {

        if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {

            if (e.getSource() == peakLists) {
                int clickedIndex = peakLists.locationToIndex(e.getPoint());
                if (clickedIndex < 0)
                    return;
                PeakList clickedPeakList = (PeakList) peakListsModel.get(clickedIndex);
                PeakListTableWindow window = new PeakListTableWindow(
                        clickedPeakList);
                desktop.addInternalFrame(window);
            }

        }

    }

    public void mouseEntered(MouseEvent e) {
        // ignore
    }

    public void mouseExited(MouseEvent e) {
        // ignore
    }

    public void mousePressed(MouseEvent e) {

        if (e.isPopupTrigger()) {
            if (e.getSource() == peakLists)
                peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }

    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            if (e.getSource() == peakLists)
                peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public void valueChanged(ListSelectionEvent event) {
    	// ignore
    }

    public void reloadDataModel() {
        MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore.getCurrentProject();

        peakListsModel = project.getPeakListsListModel();
        peakLists.setModel(peakListsModel);
        peakLists.repaint();

    }

}