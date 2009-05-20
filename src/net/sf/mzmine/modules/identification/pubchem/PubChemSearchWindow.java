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

package net.sf.mzmine.modules.identification.pubchem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.identification.pubchem.molstructureviewer.MolStructureViewer;
import net.sf.mzmine.modules.visualization.spectra.PeakListDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerType;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;

public class PubChemSearchWindow extends JInternalFrame implements
		ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PubChemResultTableModel listElementModel;
	private JButton btnAdd, btnViewer, btnIsotopeViewer, btnPubChemLink;
	private PeakListRow peakListRow;
	private JTable IDList;
	private String description;
	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	public PubChemSearchWindow(PeakListRow peakListRow) {

		super(null, true, true, true, true);

		this.peakListRow = peakListRow;

		description = "PubChem search results "
				+ massFormater.format(peakListRow.getAverageMZ());

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
		pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		pnlLabelsAndList.add(new JLabel("List of possible identities"),
				BorderLayout.NORTH);

		listElementModel = new PubChemResultTableModel();
		IDList = new JTable();
		IDList.setModel(listElementModel);
		IDList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
		btnViewer = new JButton("View structure");
		btnViewer.addActionListener(this);
		btnViewer.setActionCommand("VIEWER");
		btnIsotopeViewer = new JButton("View isotope pattern");
		btnIsotopeViewer.addActionListener(this);
		btnIsotopeViewer.setActionCommand("ISOTOPE_VIEWER");
		btnPubChemLink = new JButton("PubChem link");
		btnPubChemLink.addActionListener(this);
		btnPubChemLink.setActionCommand("PUBCHEM_LINK");

		pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		pnlButtons.add(btnAdd);
		pnlButtons.add(btnViewer);
		pnlButtons.add(btnIsotopeViewer);
		pnlButtons.add(btnPubChemLink);

		setLayout(new BorderLayout());
		setSize(500, 200);
		add(pnlLabelsAndList, BorderLayout.CENTER);
		add(pnlButtons, BorderLayout.SOUTH);
		setTitle(description);
		pack();

	}

	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		if (command.equals("ADD")) {
			int index = IDList.getSelectedRow();

			if (index < 0) {
				MZmineCore
						.getDesktop()
						.displayMessage(
								"Select one PubChem result candidate as compound identity");
				return;

			}

			peakListRow.addPeakIdentity(listElementModel.getCompoundAt(index),
					false);
			dispose();
		}

		if (command.equals("VIEWER")) {

			int index = IDList.getSelectedRow();

			if (index < 0) {
				MZmineCore
						.getDesktop()
						.displayMessage(
								"Select one PubChem result candidate to display molecule structure");
				return;

			}

			MolStructureViewer viewer;
			viewer = new MolStructureViewer(listElementModel
					.getCompoundAt(index));
			Desktop desktop = MZmineCore.getDesktop();
			desktop.addInternalFrame(viewer);

		}

		if (command.equals("ISOTOPE_VIEWER")) {

			int index = IDList.getSelectedRow();

			if (index < 0) {
				MZmineCore
						.getDesktop()
						.displayMessage(
								"Select one PubChem result candidate to display the isotope pattern");
				return;
			}

			final IsotopePattern isotopePattern = listElementModel
					.getCompoundAt(index).getIsotopePattern();

			if (isotopePattern == null)
				return;

			PeakListDataSet peakDataSet = new PeakListDataSet(isotopePattern);

			if (peakDataSet == null)
				return;

			IsotopePattern searchPattern = peakListRow.getBestIsotopePattern();

			if (searchPattern == null) {
				ChromatographicPeak bestPeakInRow = peakListRow.getBestPeak();
				RawDataFile datafile = bestPeakInRow.getDataFile();
				Scan bestScan = datafile.getScan(bestPeakInRow
						.getRepresentativeScanNumber());
				SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(
						datafile, null, SpectraVisualizerType.SPECTRUM);
				MZmineCore.getDesktop().addInternalFrame(spectraWindow);
				spectraWindow.loadRawData(bestScan);
				spectraWindow.loadIsotopePattern(isotopePattern);
			} else {
				RawDataFile datafile = searchPattern.getDataFile();
				SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(
						datafile, null, SpectraVisualizerType.ISOTOPE);
				MZmineCore.getDesktop().addInternalFrame(spectraWindow);
				spectraWindow.loadRawData(searchPattern);
				spectraWindow.loadIsotopePattern(searchPattern);
				spectraWindow.loadIsotopePattern(isotopePattern);
			}

		}

		if (command.equals("PUBCHEM_LINK")) {
			int index = IDList.getSelectedRow();

			if (index < 0) {
				MZmineCore
						.getDesktop()
						.displayMessage(
								"Select one PubChem result candidate to display in the default web browser");
				return;

			}

			logger
					.finest("Launching default browser to display PubChem compound");

			java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

			String urlString = listElementModel.getCompoundAt(index)
					.getDatabaseEntryURL();

			try {
				desktop.browse(new URI(urlString));
			} catch (Exception ex) {
				logger.severe("Error trying to launch default browser: "
						+ ex.getMessage());
			}
		}

	}

	public void addNewListItem(PubChemCompound compound) {
		int index = IDList.getSelectedRow();
		listElementModel.addElement(compound);
		if (index > -1)
			IDList.setRowSelectionInterval(index, index);
	}

	public String toString() {
		return description;
	}

}
