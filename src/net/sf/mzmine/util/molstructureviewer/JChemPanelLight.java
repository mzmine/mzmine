package net.sf.mzmine.util.molstructureviewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;

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
	
	public static JPanel getEmptyPanelWithModel()
	{
		JChemPaintModel model = new JChemPaintModel();

		JChemPanelLight jcpep = new JChemPanelLight();
		jcpep.registerModel(model);
		jcpep.setJChemPaintModel(model,null);
		model.getControllerModel().setAutoUpdateImplicitHydrogens(true);
		model.getRendererModel().setShowEndCarbons(true);
		model.getControllerModel().setDrawMode(
				Controller2DModel.LASSO);
		model.getControllerModel().setMovingAllowed(false);
		
		//jcpep.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//jcpep.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		//JPanel jcpf = getNewPanel(model);
		return jcpep;
	}
	
	public static JPanel getNewPanel(JChemPaintModel model)
	{
		JPanel panel = new JPanel();
		JChemPanelLight jcpep = new JChemPanelLight();
		panel.add(jcpep);
		jcpep.registerModel(model);
		jcpep.setJChemPaintModel(model,null);
		//frame.setTitle(model.getTitle());
		//This ensures that the drawingpanel is never smaller than the application
		panel.addComponentListener(new ComponentAdapter(){
			public void componentResized(ComponentEvent e) {
				if(((JChemPaintEditorPanel)((JFrame)e.getSource()).getContentPane().getComponent(0)).getJChemPaintModel().getRendererModel().getBackgroundDimension().width<((JFrame)e.getSource()).getWidth()-30)
					((JChemPaintEditorPanel)((JFrame)e.getSource()).getContentPane().getComponent(0)).getJChemPaintModel().getRendererModel().setBackgroundDimension(new Dimension(((JFrame)e.getSource()).getWidth()-30,((JChemPaintEditorPanel)((JFrame)e.getSource()).getContentPane().getComponent(0)).getJChemPaintModel().getRendererModel().getBackgroundDimension().height));
				if(((JChemPaintEditorPanel)((JFrame)e.getSource()).getContentPane().getComponent(0)).getJChemPaintModel().getRendererModel().getBackgroundDimension().height<((JFrame)e.getSource()).getHeight()-30)
					((JChemPaintEditorPanel)((JFrame)e.getSource()).getContentPane().getComponent(0)).getJChemPaintModel().getRendererModel().setBackgroundDimension(new Dimension(((JChemPaintEditorPanel)((JFrame)e.getSource()).getContentPane().getComponent(0)).getJChemPaintModel().getRendererModel().getBackgroundDimension().width,((JFrame)e.getSource()).getHeight()-30));
			}
		});
		model.getControllerModel().setAutoUpdateImplicitHydrogens(true);
		model.getRendererModel().setShowEndCarbons(true);
		model.getControllerModel().setDrawMode(
				Controller2DModel.LASSO);
		model.getControllerModel().setMovingAllowed(false);
		
		jcpep.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		jcpep.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);


		return panel;
	}	

    @Override public void stateChanged(ChangeEvent e)
    {
		updateStatusBar();
    }

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

	public String getStatus(int position) {
		String status = "";
		if (position == 0) {
			status = "Unknown";
		} else if (position == 1) {
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
				int hs = 0;
				atom = wholeModel.getAtom(f);
				mass += (double) atom.getExactMass();
			}

			status = "<html>" + formula + "  Mass: " + mass + "</html>";
		} else if (position == 2) {
			Renderer2DModel rendererModel = getJChemPaintModel()
					.getRendererModel();
			if (rendererModel.getSelectedPart() != null) {
				IAtomContainer selectedPart = rendererModel.getSelectedPart();
				String formula = new MFAnalyser(selectedPart, true)
						.getHTMLMolecularFormulaWithCharge();
				double mass = 0;
				IAtom atom = null;
				String eval = "";
				for (int f = 0; f < selectedPart.getAtomCount(); f++) {
					atom = selectedPart.getAtom(f);
					eval += "select  atomX=" + atom.getPoint2d().x;
                    eval += "; color atom yellow;";
					mass += (double) atom.getExactMass();
				}
				status = "<html>" + formula + "  Mass: " + mass + "</html>";
				if (jmolPanel != null){
					if (selectedPart.getAtomCount() > 0)
						jmolPanel.getViewer().evalString(eval);
					else
						jmolPanel.getViewer().evalString("select all; color cpk");
				}
				
			}
			else {
				if (jmolPanel != null)
				jmolPanel.getViewer().evalString("select all; color cpk");
			}
		}
		return status;
	}
	
	public void setJmolPanel(JMolPanelLight jmolPanel){
		this.jmolPanel = jmolPanel;
	}
	
	public void setCompoundName(String compoundName){
		this.compoundName = compoundName;
	}
	
	@Override 	public void setupPopupMenus(PopupController2D inputAdapter)
	{
	}

}
