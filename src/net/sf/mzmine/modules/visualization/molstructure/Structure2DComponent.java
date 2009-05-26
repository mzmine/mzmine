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

package net.sf.mzmine.modules.visualization.molstructure;

import java.awt.Dimension;
import java.awt.Point;
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.IsotopeUtils;

import org.openscience.cdk.ChemModel;
import org.openscience.cdk.applications.jchempaint.JChemPaintEditorPanel;
import org.openscience.cdk.applications.jchempaint.JChemPaintModel;
import org.openscience.cdk.controller.Controller2DModel;
import org.openscience.cdk.controller.PopupController2D;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.renderer.Renderer2DModel;
import org.openscience.cdk.tools.HydrogenAdder;
import org.openscience.cdk.tools.MFAnalyser;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;

public class Structure2DComponent extends JChemPaintEditorPanel {

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();
	private JLabel statusLabel;

	public Structure2DComponent(String structure) throws CDKException {
		this(structure, null);
	}

	public Structure2DComponent(String structure, JLabel statusLabel)
			throws CDKException {

		this.statusLabel = statusLabel;

		// Set panel properties
		setShowStatusBar(false);
		setShowInsertTextField(false);
		setShowMenuBar(false);
		setShowToolBar(false);

		// It is necessary to change the size of the canvas BEFORE we load the
		// structure
		getJChemPaintModel().getRendererModel().setBackgroundDimension(
				new Dimension(2000, 2000));

		// Load the structure
		StringReader reader = new StringReader(structure);
		MDLV2000Reader molReader = new MDLV2000Reader(reader);
		ChemModel chemModel = new ChemModel();
		chemModel = (ChemModel) molReader.read(chemModel);

		// Add implicit hydrogens which may be missing in the structure. This is
		// necessary for correct mass calculation.
		HydrogenAdder hydrogenAdder = new HydrogenAdder(
				"org.openscience.cdk.tools.ValencyChecker");
		Iterator mols = chemModel.getMoleculeSet().molecules();
		while (mols.hasNext()) {
			IMolecule molecule = (IMolecule) mols.next();
			hydrogenAdder.addImplicitHydrogensToSatisfyValency(molecule);
		}

		// Load the model
		processChemModel(chemModel);

		// Get JChemPaintModel
		JChemPaintModel jcpModel = getJChemPaintModel();

		// Set renderer properties
		Renderer2DModel renderer = jcpModel.getRendererModel();
		renderer.setShowEndCarbons(true);
		renderer.setShowExplicitHydrogens(true);
		renderer.setShowImplicitHydrogens(true);
		renderer.setZoomFactor(0.9);

		// Set controller properties
		Controller2DModel controller = jcpModel.getControllerModel();
		controller.setDrawMode(Controller2DModel.LASSO);
		controller.setMovingAllowed(false);

		// Scroll to the center
		JViewport vp = getScrollPane().getViewport();
		vp.setViewPosition(new Point(700, 700));

	}

	/**
	 * Override method to restrict functionality
	 */
	@Override
	public void setupPopupMenus(PopupController2D inputAdapter) {
	}

	/**
	 * Override method to restrict functionality
	 */
	@Override
	public void stateChanged(ChangeEvent e) {

		super.stateChanged(e);

		if (statusLabel == null)
			return;

		// Get the formula and mass of complete molecule
		IChemModel model = getJChemPaintModel().getChemModel();
		IAtomContainer wholeModel = model.getBuilder().newAtomContainer();
		Iterator containers = ChemModelManipulator.getAllAtomContainers(model)
				.iterator();

		while (containers.hasNext()) {
			wholeModel.add((IAtomContainer) containers.next());
		}

		MFAnalyser formulaAnalyzer = new MFAnalyser(wholeModel, true);

		String wholeFormula = formulaAnalyzer
				.getHTMLMolecularFormulaWithCharge();

		// Unfortunately, the mass returned by formulaAnalyzer.getMass() is not
		// precise, so we have to calculate it with own our method, using the
		// molecular formula
		double wholeMass = IsotopeUtils.calculateExactMass(formulaAnalyzer
				.getMolecularFormula());

		StringBuilder status = new StringBuilder("<html>Formula: ");
		status.append(wholeFormula);
		status.append(",  mass: ");
		status.append(massFormater.format(wholeMass));
		status.append(" amu");

		Renderer2DModel rendererModel = getJChemPaintModel().getRendererModel();

		IAtomContainer selectedPart = rendererModel.getSelectedPart();

		if ((selectedPart != null) && (selectedPart.getAtomCount() > 0)) {

			MFAnalyser selectionAnalyzer = new MFAnalyser(selectedPart, true);

			String selectionFormula = selectionAnalyzer.getMolecularFormula();
			String selectionHTMLFormula = selectionAnalyzer
					.getHTMLMolecularFormulaWithCharge();
			double selectionMass = IsotopeUtils
					.calculateExactMass(selectionFormula);

			status.append("; selected formula: ");
			status.append(selectionHTMLFormula);
			status.append(", mass: ");
			status.append(massFormater.format(selectionMass));
			status.append(" amu");

		}

		status.append("</html>");

		statusLabel.setText(status.toString());

	}

}
