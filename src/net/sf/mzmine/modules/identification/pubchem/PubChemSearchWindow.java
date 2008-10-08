/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.identification.pubchem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.PeakListDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.util.molstructureviewer.MolStructureViewer;

public class PubChemSearchWindow extends JInternalFrame implements
        ActionListener {

    private PubChemResultTableModel listElementModel;
    private JButton btnAdd, btnAddAll, btnViewer, btnIsotopeViewer;
    private PeakListRow peakListRow;
    private JTable IDList;
    private ChromatographicPeak peak;


    public PubChemSearchWindow(PeakListRow peakListRow, ChromatographicPeak peak) {

        super(null, true, true, true, true);
        
        this.peakListRow = peakListRow;
        this.peak = peak;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
        pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        pnlLabelsAndList.add(new JLabel("List of possible identities"),
                BorderLayout.NORTH);

        listElementModel = new PubChemResultTableModel();
        IDList = new JTable();
        IDList.setModel(listElementModel);
        JScrollPane listScroller = new JScrollPane(IDList);
        listScroller.setPreferredSize(new Dimension(350, 100));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
        listPanel.add(listScroller);
        listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel();
        btnAdd = new JButton("Add identity");
        btnAdd.addActionListener(this);
        btnAdd.setActionCommand("ADD");
        btnAddAll = new JButton("Add all");
        btnAddAll.addActionListener(this);
        btnAddAll.setActionCommand("ADD_ALL");
        btnViewer = new JButton("View structure");
        btnViewer.addActionListener(this);
        btnViewer.setActionCommand("VIEWER");
        btnIsotopeViewer = new JButton("View isotope pattern");
        btnIsotopeViewer.addActionListener(this);
        btnIsotopeViewer.setActionCommand("ISOTOPE_VIEWER");
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnlButtons.add(btnAdd);
        pnlButtons.add(btnAddAll);
        pnlButtons.add(btnViewer);
        pnlButtons.add(btnIsotopeViewer);

        setLayout(new BorderLayout());
        setSize(500, 200);
        add(pnlLabelsAndList, BorderLayout.CENTER);
        add(pnlButtons, BorderLayout.SOUTH);
        pack();

    }

    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        if (command.equals("ADD")) {
            int[] indices = IDList.getSelectedRows();
            for (int ind : indices) {
                peakListRow.addCompoundIdentity(listElementModel.getElementAt(ind));
            }
            dispose();
        }

        if (command.equals("ADD_ALL")) {
            int length = listElementModel.getRowCount();
            for (int i = 0; i < length; i++) {
                peakListRow.addCompoundIdentity(listElementModel.getElementAt(i));
            }
            dispose();
        }

        if (command.equals("VIEWER")) {
            int[] indices = IDList.getSelectedRows();
            MolStructureViewer viewer;
            String CID, name;
            for (int ind : indices) {
                CID = (String) listElementModel.getValueAt(ind,0);
                name = (String) listElementModel.getValueAt(ind,1);
                viewer = new MolStructureViewer(CID, name);
				Desktop desktop = MZmineCore.getDesktop();
				desktop.addInternalFrame(viewer);
            }
        }

        if (command.equals("ISOTOPE_VIEWER")) {

            if (!(peak instanceof IsotopePattern)){
            	MZmineCore.getDesktop()
				.displayMessage("The selected peak does not represent an isotope pattern.");
            	return;
            }
            
            int[] indices = IDList.getSelectedRows();

            SpectraVisualizer specVis = SpectraVisualizer.getInstance();
        	SpectraVisualizerWindow spectraWindow;
        	IsotopePattern isotopePattern;
        	PeakListDataSet peakDataSet;
        	
            for (int ind : indices) {
            	
            	if (listElementModel.getValueAt(ind, 4).equals(""))
            		continue;
            	
            	isotopePattern = listElementModel.getElementAt(ind).getIsotopePattern();
            	
            	if (isotopePattern == null)
            		continue;

            	peakDataSet = new PeakListDataSet(isotopePattern);
            	
            	if (peakDataSet == null)
            		continue;
            	
            	spectraWindow = specVis.showNewSpectrumWindow(peak.getDataFile(), (IsotopePattern) peak);
            	spectraWindow.getSpectrumPlot().addPeaksDataSet(new PeakListDataSet((IsotopePattern) peak));
            	spectraWindow.getSpectrumPlot().addPeaksDataSet(peakDataSet);
            	
            }
        }
    }

    public void addNewListItem(CompoundIdentity compound) {
        listElementModel.addElement(compound);
    }

}
