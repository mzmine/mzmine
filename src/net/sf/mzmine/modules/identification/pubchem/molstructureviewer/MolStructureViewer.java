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

package net.sf.mzmine.modules.identification.pubchem.molstructureviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.identification.pubchem.PubChemCompound;

import org.jmol.api.JmolViewer;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.controller.Controller2DModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.HydrogenAdder;

public class MolStructureViewer extends JInternalFrame implements
		ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private JMolPanelLight jmolPanel;
	private JChemPanelLight jcp;
	private String description, structure3D;
	private PubChemCompound compound;
	private static int openDialogCount = 0;
	private static final int xOffset = 30, yOffset = 30;
	private static final Font buttonFont = new Font("SansSerif", Font.BOLD, 11);
	private static final String pub3dAddress = "http://www.chembiogrid.org/cheminfo/rest/db/pub3d/";
	private static final String chembiogrid = "Load 3D structure from Pub3d";
	private static final String pubchem = "Load 2D structure from PubChem";

	/**
	 * This class extends JInternalFrame. This frame contains two major panels
	 * for visualization of chemical structure
	 * 
	 * @param compound
	 */
	public MolStructureViewer(PubChemCompound compound) {

		super("Structure " + compound.getCompoundName() + " CID"
				+ compound.getCompoundID(), true, true, true, true);

		this.compound = compound;

		this.description = "Structure " + compound.getCompoundName() + " CID"
				+ compound.getCompoundID();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		// Initialize visualizers
		jcp = (JChemPanelLight) JChemPanelLight.getEmptyPanelWithModel();

		jmolPanel = new JMolPanelLight();
		jmolPanel.setPreferredSize(new Dimension(400, 400));
		jmolPanel.setBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.RAISED));

		// Add button for 3D visualization
		JPanel jmolAndButton = new JPanel(new BorderLayout());
		JButton button = new JButton(chembiogrid);
		button.addActionListener(this);
		button.setFont(buttonFont);
		jmolAndButton.add(jmolPanel, BorderLayout.CENTER);
		jmolAndButton.add(button, BorderLayout.SOUTH);

		JPanel container = (JPanel) jcp.getScrollPane().getParent();
		container.add(jmolAndButton, BorderLayout.WEST);

		String compoundName = compound.getCompoundName();

		JLabel labelName = new JLabel(compoundName + " ("
				+ compound.getCompoundFormula() + ")", SwingConstants.CENTER);
		labelName.setOpaque(true);
		labelName.setBackground(Color.WHITE);
		labelName.setForeground(Color.BLUE);
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);

		labelName.setBorder(BorderFactory.createCompoundBorder(one, two));
		labelName.setFont(new Font("SansSerif", Font.BOLD, 20));
		container.add(labelName, BorderLayout.NORTH);

		openDialogCount++;
		setLocation(xOffset * openDialogCount, yOffset * openDialogCount);

		// Load structure into visualizers
		try {

			setJmolViewerStructure(compound.getStructure());
			setJChemViewerStrucuture(compound.getStructure());

			jcp.setJmolPanel(jmolPanel);
			jcp.setCompoundName(compoundName);
			pack();

		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Error trying to load chemical structure for visualization " + e.getMessage());
		}

		setPreferredSize(new Dimension(1000, 600));
		add(jcp);
		pack();

	}

	/**
	 * Load the structure passed as parameter in JmolViewer
	 * 
	 * @param structure
	 */
	private void setJmolViewerStructure(String structure) {

		JmolViewer viewer = jmolPanel.getViewer();
		try {
			viewer.loadInline(structure);
			// viewer
			// .evalString(
			// "select all; delay 1; move 90 0 0 0 0 0 0 0 5; delay 1; move -90 0 0 0 0 0 0 0 5;"
			// );
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Error trying to load chemical structure in Jmol " + e.getMessage());
		}

	}

	/**
	 * Load the structure passed as parameter in JChemViewer
	 * 
	 * @param structure
	 * @throws Exception
	 */
	private void setJChemViewerStrucuture(String structure) throws Exception {

		ByteArrayInputStream reader = new ByteArrayInputStream(structure
				.getBytes());
		IChemObjectReader cor = new MDLV2000Reader(reader);
		ChemModel chemModel = (ChemModel) cor
				.read((IChemObject) new ChemModel());
		if (chemModel != null) {
			jcp.processChemModel(chemModel);
			if (jcp.getJChemPaintModel().getControllerModel()
					.getAutoUpdateImplicitHydrogens()) {
				HydrogenAdder hydrogenAdder = new HydrogenAdder(
						"org.openscience.cdk.tools.ValencyChecker");
				Iterator mols = chemModel.getMoleculeSet().molecules();
				while (mols.hasNext()) {
					IMolecule molecule = (IMolecule) mols.next();
					if (molecule != null) {
						hydrogenAdder
								.addImplicitHydrogensToSatisfyValency(molecule);
					}
				}
			}

			// The following do apply either to the existing or the new
			// frame
			jcp.lastUsedJCPP.getJChemPaintModel().setTitle(
					"CID_" + compound.getCompoundID() + ".sdf");
			jcp.getJChemPaintModel().getControllerModel().setDrawMode(
					Controller2DModel.LASSO);
			jcp.getJChemPaintModel().getControllerModel().setMovingAllowed(
					false);
			jcp.getJChemPaintModel().getRendererModel()
					.setShowExplicitHydrogens(true);
			jcp.getJChemPaintModel().getRendererModel()
					.setShowImplicitHydrogens(true);
			jcp.getScrollPane().getViewport().setViewPosition(
					new java.awt.Point(115, 320));

		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source instanceof JButton) {
			JButton b = (JButton) source;
			if (b.getText().equals(chembiogrid)) {

				// Request 3D structure
				if (structure3D == null) {
					try {
						structure3D = get3DStructure(compound.getCompoundID());
					} catch (Exception e1) {
						e1.printStackTrace();
						MZmineCore.getDesktop().displayMessage(
								"The Pub3D does not contain this structure.");
						return;
					}
				}

				setJmolViewerStructure(structure3D);
				b.setText(pubchem);
			} else {
				setJmolViewerStructure(compound.getStructure());
				b.setText(chembiogrid);
			}
		}

	}

	/**
	 * Retrieve 3D structure from Pub3d server
	 * 
	 * @param ID
	 * @return
	 * @throws Exception
	 */
	private static String get3DStructure(String ID) throws Exception {

		URL url = new URL(pub3dAddress + ID);

		InputStream in = url.openStream();

		if (in == null) {
			throw new Exception("Got a null content PubChem connection!");
		}

		BufferedReader is = new BufferedReader(new InputStreamReader(in,
				"UTF-8"));
		String responseLine, structure = "";

		while ((responseLine = is.readLine()) != null) {
			structure += responseLine + "\n";
		}

		is.close();
		return structure;

	}

	/**
	 * Returns a description of the current object
	 */
	public String toString() {
		return description;
	}

}
