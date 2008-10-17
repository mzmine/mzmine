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
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.identification.pubchem.molstructureviewer.MolStructureViewer;
import net.sf.mzmine.modules.visualization.spectra.PeakListDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerType;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class PubChemSearchWindow extends JInternalFrame implements
		ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PubChemResultTableModel listElementModel;
	private JButton btnAdd, btnViewer, btnIsotopeViewer, btnPubChemLink;
	private PeakListRow peakListRow;
	private JTable IDList;
	private ChromatographicPeak peak;
	private String description;
	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	public PubChemSearchWindow(PeakListRow peakListRow, ChromatographicPeak peak) {

		super(null, true, true, true, true);

		this.peakListRow = peakListRow;
		this.peak = peak;

		description = "PubChem search results "
				+ massFormater.format(peak.getMZ());

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
			peakListRow.addCompoundIdentity(listElementModel
					.getElementAt(index));
			dispose();
		}

		if (command.equals("VIEWER")) {

			int index = IDList.getSelectedRow();
			MolStructureViewer viewer;
			viewer = new MolStructureViewer(listElementModel
					.getElementAt(index));
			Desktop desktop = MZmineCore.getDesktop();
			desktop.addInternalFrame(viewer);

		}

		if (command.equals("ISOTOPE_VIEWER")) {

			if (!(peak instanceof IsotopePattern)) {
				MZmineCore
						.getDesktop()
						.displayMessage(
								"The selected peak does not represent an isotope pattern.");
				return;
			}

			int index = IDList.getSelectedRow();

			if (listElementModel.getValueAt(index, 4) == null) {
				MZmineCore
						.getDesktop()
						.displayMessage(
								"The selected result compound does not have a predicted isotope pattern."
										+ " Please repite the search with \"Isotope Pattern filter\" checked");
				return;
			}

			final IsotopePattern isotopePattern = listElementModel
					.getElementAt(index).getIsotopePattern();

			if (isotopePattern == null)
				return;

			PeakListDataSet peakDataSet = new PeakListDataSet(isotopePattern);

			if (peakDataSet == null)
				return;

			final SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(
					peak.getDataFile(), null, SpectraVisualizerType.ISOTOPE);
	        
			MZmineCore.getDesktop().addInternalFrame(spectraWindow);

			Runnable newThreadRunnable = new Runnable() {

				public void run() {			
					spectraWindow.loadRawData((IsotopePattern) peak);
					spectraWindow.loadIsotopePattern((IsotopePattern) peak);
					spectraWindow.loadIsotopePattern(isotopePattern);
				}

			};

			Thread newThread = new Thread(newThreadRunnable);
			newThread.start();

		}

		if (command.equals("PUBCHEM_LINK")) {
			int index = IDList.getSelectedRow();
			logger
					.finest("Launching default browser to display PubChem compound");
			try {
				BrowserLauncher launcher = new BrowserLauncher();
				launcher.setNewWindowPolicy(false);

				String urlString = listElementModel.getElementAt(index)
						.getDatabaseEntryURL();
				launcher.openURLinBrowser("DEFAULT", urlString);

			} catch (BrowserLaunchingInitializingException e1) {
				e1.printStackTrace();
				logger.severe(" Error trying to launch default browser "
						+ e1.getMessage());
			} catch (UnsupportedOperatingSystemException e1) {
				e1.printStackTrace();
				logger.severe(" Error trying to launch default browser "
						+ e1.getMessage());
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
