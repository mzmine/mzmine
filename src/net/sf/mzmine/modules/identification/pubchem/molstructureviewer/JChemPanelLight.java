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

package net.sf.mzmine.modules.identification.pubchem.molstructureviewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.NumberFormat;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;

import net.sf.mzmine.main.MZmineCore;

import org.openscience.cdk.applications.jchempaint.JChemPaintEditorPanel;
import org.openscience.cdk.applications.jchempaint.JChemPaintModel;
import org.openscience.cdk.applications.jchempaint.StatusBar;
import org.openscience.cdk.controller.Controller2DModel;
import org.openscience.cdk.controller.PopupController2D;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.renderer.Renderer2DModel;
import org.openscience.cdk.tools.MFAnalyser;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;

public class JChemPanelLight extends JChemPaintEditorPanel {

	private JMolPanelLight jmolPanel;
	private String compoundName;
	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	/**
	 * This method initialize a JChemPanelLight object and registers a new model
	 * 
	 * @return JPanel jmolViewer
	 */
	public static JPanel getEmptyPanelWithModel() {

		JChemPaintModel model = new JChemPaintModel();
		
		model.getControllerModel().setAutoUpdateImplicitHydrogens(true);
		model.getControllerModel().setDrawMode(Controller2DModel.LASSO);
		model.getControllerModel().setMovingAllowed(false);
		
		model.getRendererModel().setShowEndCarbons(true);

		JChemPanelLight jcpep = new JChemPanelLight();
		jcpep.registerModel(model);
		jcpep.setJChemPaintModel(model, null);
		jcpep.setShowInsertTextField(false);
		jcpep.setShowMenuBar(false);
		jcpep.setShowToolBar(false);
		jcpep.revalidate();

		return jcpep;
	}

	/**
	 * This method returns a JPanel with a JChemPanelLight object. The
	 * JChemPanelLight is initialized with the model passed as parameter
	 * 
	 * @param model
	 * @return JPanel jmolViewer
	 */
	public static JPanel getNewPanel(JChemPaintModel model) {
		JPanel panel = new JPanel();
		JChemPanelLight jcpep = new JChemPanelLight();
		panel.add(jcpep);
		jcpep.registerModel(model);
		jcpep.setJChemPaintModel(model, null);

		// This ensures that the drawing panel is never smaller than the
		// application
		panel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (((JChemPaintEditorPanel) ((JFrame) e.getSource())
						.getContentPane().getComponent(0)).getJChemPaintModel()
						.getRendererModel().getBackgroundDimension().width < ((JFrame) e
						.getSource()).getWidth() - 30)
					((JChemPaintEditorPanel) ((JFrame) e.getSource())
							.getContentPane().getComponent(0))
							.getJChemPaintModel()
							.getRendererModel()
							.setBackgroundDimension(
									new Dimension(
											((JFrame) e.getSource()).getWidth() - 30,
											((JChemPaintEditorPanel) ((JFrame) e
													.getSource())
													.getContentPane()
													.getComponent(0))
													.getJChemPaintModel()
													.getRendererModel()
													.getBackgroundDimension().height));
				if (((JChemPaintEditorPanel) ((JFrame) e.getSource())
						.getContentPane().getComponent(0)).getJChemPaintModel()
						.getRendererModel().getBackgroundDimension().height < ((JFrame) e
						.getSource()).getHeight() - 30)
					((JChemPaintEditorPanel) ((JFrame) e.getSource())
							.getContentPane().getComponent(0))
							.getJChemPaintModel()
							.getRendererModel()
							.setBackgroundDimension(
									new Dimension(
											((JChemPaintEditorPanel) ((JFrame) e
													.getSource())
													.getContentPane()
													.getComponent(0))
													.getJChemPaintModel()
													.getRendererModel()
													.getBackgroundDimension().width,
											((JFrame) e.getSource())
													.getHeight() - 30));
			}
		});
		model.getControllerModel().setAutoUpdateImplicitHydrogens(true);
		model.getRendererModel().setShowEndCarbons(true);
		model.getControllerModel().setDrawMode(Controller2DModel.LASSO);
		model.getControllerModel().setMovingAllowed(false);

		return panel;
	}

	/**
	 * Override method to restrict functionality
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		updateStatusBar();
	}

	/**
	 * Override method to restrict functionality
	 */
	@Override
	public void setupPopupMenus(PopupController2D inputAdapter) {
	}

	/**
	 * This method updates the text in the status bar.
	 * 
	 */
	public void updateStatusBar() {
		StatusBar statusbar = new StatusBar();
		Component[] components = ((JPanel) this).getComponents();
		for (Component comp : components) {
			if (comp instanceof StatusBar) {
				statusbar = (StatusBar) comp;
			}
		}
		statusbar.setStatus(1, compoundName);
		for (int i = 1; i < 3; i++) {
			String status = getStatus(i);
			statusbar.setStatus(i + 1, status);
		}
	}

	/**
	 * This method return a string with the information regarding the asked
	 * position
	 * 
	 * @param position
	 * @return String
	 */
	public String getStatus(int position) {
		String status = "";
		if (position == 0) {
			status = "Unknown";
		} else if (position == 1) {

			// Get the formula and mass of complete molecule
			IChemModel model = getJChemPaintModel().getChemModel();
			IAtomContainer wholeModel = model.getBuilder().newAtomContainer();
			Iterator containers = ChemModelManipulator.getAllAtomContainers(
					model).iterator();

			while (containers.hasNext()) {
				wholeModel.add((IAtomContainer) containers.next());
			}

			String formula = new MFAnalyser(wholeModel, true)
					.getHTMLMolecularFormulaWithCharge();

			double mass = 0;
			IAtom atom = null;
			for (int f = 0; f < wholeModel.getAtomCount(); f++) {
				atom = wholeModel.getAtom(f);
				mass += (double) atom.getExactMass();
			}

			status = "<html>" + formula + "  Mass: "
					+ massFormater.format(mass) + "</html>";

		} else if (position == 2) {

			// Get the formula and mass of selected portion in the molecule
			Renderer2DModel rendererModel = getJChemPaintModel()
					.getRendererModel();

			if (rendererModel.getSelectedPart() != null) {

				// Clean previous selection
				jmolPanel.getViewer().evalString("select all; color cpk");

				IAtomContainer selectedPart = rendererModel.getSelectedPart();
				String formula = new MFAnalyser(selectedPart, true)
						.getHTMLMolecularFormulaWithCharge();
				double mass = 0;
				IAtom atom = null;
				String eval = "";
				for (int f = 0; f < selectedPart.getAtomCount(); f++) {
					atom = selectedPart.getAtom(f);
					eval += "select  (atomX=" + atom.getPoint2d().x;
					eval += " and  atomY=" + atom.getPoint2d().y;
					eval += "); color atom yellow;";
					mass += (double) atom.getExactMass();
				}

				status = "<html>" + formula + "  Mass: "
						+ massFormater.format(mass) + "</html>";

				// Change the color of selected atoms to yellow in jmolViewer
				if (jmolPanel != null) {
					if (selectedPart.getAtomCount() > 0)
						jmolPanel.getViewer().evalString(eval);
					else
						jmolPanel.getViewer().evalString(
								"select all; color cpk");
				}

			}

			else {
				// In case of no selected atom exists, the jmolViewer display
				// atoms in original colors
				if (jmolPanel != null)
					jmolPanel.getViewer().evalString("select all; color cpk");
			}
		}
		return status;
	}

	/**
	 * Establish the relationship between this JChemPanel and JmolViewer to
	 * provide visualization effects (atom selection)
	 * 
	 * @param jmolPanel
	 */
	public void setJmolPanel(JMolPanelLight jmolPanel) {
		this.jmolPanel = jmolPanel;
	}

	/**
	 * Set the name of the visualized compound
	 * 
	 * @param compoundName
	 */
	public void setCompoundName(String compoundName) {
		this.compoundName = compoundName;
	}

}
