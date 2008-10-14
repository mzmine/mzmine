package net.sf.mzmine.modules.identification.pubchem.molstructureviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.modules.identification.pubchem.PubChemCompound;

import org.jmol.api.JmolViewer;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.controller.Controller2DModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.HydrogenAdder;

public class MolStructureViewer extends JInternalFrame{
	
	private JMolPanelLight jmolPanel;
	private JChemPanelLight jcp;
	private File inFile;
    private static int openDialogCount = 0;
    private static final int xOffset = 30, yOffset = 30;
	
	public MolStructureViewer(PubChemCompound compound ){

		super("Molecular structure visualization of CID " + compound.getCompoundID(), true, true, true, true);
		
		
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

		jcp = (JChemPanelLight) JChemPanelLight.getEmptyPanelWithModel();
		jcp.setShowInsertTextField(false);
		jcp.setShowMenuBar(false);
		jcp.setShowToolBar(false);
		jcp.revalidate();


		jmolPanel = new JMolPanelLight();
		jmolPanel.setPreferredSize(new Dimension(400, 400));
		jmolPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		JPanel container = (JPanel) jcp.getScrollPane().getParent();
		container.add(jmolPanel, BorderLayout.WEST);
		
		String compoundName = compound.getCompoundName();
		
		JLabel labelName = new JLabel(compoundName,SwingConstants.CENTER);
		labelName.setOpaque(true);
		labelName.setBackground(Color.WHITE);
		labelName.setForeground(Color.BLUE);
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		
		labelName.setBorder(BorderFactory.createCompoundBorder(one, two));
		labelName.setFont(new Font("SansSerif", Font.BOLD, 20));
		container.add(labelName, BorderLayout.NORTH);
		
		openDialogCount++;
		setLocation(xOffset*openDialogCount, yOffset*openDialogCount);

		try {
			
			setJmolViewerStructure(compound.getStructure());
			
			ByteArrayInputStream reader = new ByteArrayInputStream(compound.getStructure().getBytes());
			IChemObjectReader cor = new MDLV2000Reader(reader);
			ChemModel chemModel = (ChemModel) cor.read((IChemObject) new ChemModel());
			if (chemModel != null) {
				jcp.processChemModel(chemModel);
				if(jcp.getJChemPaintModel().getControllerModel().getAutoUpdateImplicitHydrogens()){
					HydrogenAdder hydrogenAdder = new HydrogenAdder("org.openscience.cdk.tools.ValencyChecker");
		        	Iterator mols = chemModel.getMoleculeSet().molecules();
					while (mols.hasNext())
					{
						IMolecule molecule = (IMolecule)mols.next();
					    if (molecule != null)
						{
							try{
									hydrogenAdder.addImplicitHydrogensToSatisfyValency(molecule);
							}catch(Exception ex){
								//do nothing
							}
						}
					}
				}
				
				//The following do apply either to the existing or the new frame
				jcp.lastUsedJCPP.getJChemPaintModel().setTitle("CID_" + compound.getCompoundID() + ".sdf");
				jcp.lastUsedJCPP.setIsAlreadyAFile(inFile);
				jcp.getJChemPaintModel().getControllerModel().setDrawMode(
						Controller2DModel.LASSO);
				jcp.getJChemPaintModel().getControllerModel().setMovingAllowed(false);
				jcp.getJChemPaintModel().getRendererModel().setShowExplicitHydrogens(true);
				jcp.getJChemPaintModel().getRendererModel().setShowImplicitHydrogens(true);


			}

			jcp.setJmolPanel(jmolPanel);
			jcp.setCompoundName(compoundName);
			pack();

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		setPreferredSize(new Dimension(1000, 500));
		add(jcp);
		pack();
		
	}

	
	public void setJmolViewerStructure(String structure){

		JmolViewer viewer = jmolPanel.getViewer();
		try {
			viewer.loadInline(structure);
			viewer.evalString("select all; delay 1; move 180 0 0 0 0 0 0 0 5; delay 1; move -180 0 0 0 0 0 0 0 5;");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
