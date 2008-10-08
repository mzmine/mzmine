package net.sf.mzmine.util.molstructureviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.jmol.api.JmolViewer;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.controller.Controller2DModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.HydrogenAdder;

public class MolStructureViewer extends JInternalFrame implements InternalFrameListener {
	
	private JMolPanelLight jmolPanel;
	private JChemPanelLight jcp;
	private File inFile;
    private static int openDialogCount = 0;
    private static final int xOffset = 30, yOffset = 30;
    private String fileName;
	
	public MolStructureViewer(String cid, String compoundName){

		super("Molecular structure visualization of CID " + cid, true, true, true, true);
		
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        addInternalFrameListener(this);

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
		setVisible(true);

		URL url;
		try {
			url = new URL(
					"http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="+cid+"&disopt=DisplaySDF");
			InputStream in=url.openStream ();
			
			fileName = "temp" + this.hashCode() + ".sdf";
			File inFile = writeFile(in, fileName);
			FileInputStream reader = new FileInputStream(inFile);
			IChemObjectReader cor = new MDLV2000Reader(reader);
			ChemModel chemModel = (ChemModel) cor.read((IChemObject) new ChemModel());
			if (chemModel != null) {
				jcp.processChemModel(chemModel);
				if(jcp.getJChemPaintModel().getControllerModel().getAutoUpdateImplicitHydrogens()){
					HydrogenAdder hydrogenAdder = new HydrogenAdder("org.openscience.cdk.tools.ValencyChecker");
		        	java.util.Iterator mols = chemModel.getMoleculeSet().molecules();
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
				jcp.lastUsedJCPP.getJChemPaintModel().setTitle("CID_5957.sdf");
				jcp.lastUsedJCPP.setIsAlreadyAFile(inFile);
				jcp.getJChemPaintModel().getControllerModel().setDrawMode(
						Controller2DModel.LASSO);
				jcp.getJChemPaintModel().getControllerModel().setMovingAllowed(false);
				jcp.getJChemPaintModel().getRendererModel().setShowExplicitHydrogens(true);
				jcp.getJChemPaintModel().getRendererModel().setShowImplicitHydrogens(true);


			}
			setStructure(inFile);
			jcp.setJmolPanel(jmolPanel);
			jcp.setCompoundName(compoundName);
			this.inFile = inFile;
			pack();

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		setPreferredSize(new Dimension(1000, 500));
		add(jcp);
		pack();
		
	}

	
	private File writeFile(InputStream inStream, String fileName) throws IOException {
		File file = new File(fileName);
	    final int bufferSize = 1000;
	    inStream = new BufferedInputStream(inStream);
	    BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(file));
	    fout.flush();
	    byte[] buffer = new byte[bufferSize];
	    int readCount = 0;
	    while ((readCount = inStream.read(buffer)) != -1) { 
	      if (readCount < bufferSize) {
	        fout.write(buffer, 0, readCount);
	      } else {
	        fout.write(buffer);
	      }
	    }
	    fout.close();
	    inStream.close();
	    return file;
	  }
	
	public void setStructure(File infile) {

		JmolViewer viewer = jmolPanel.getViewer();
		try {
			viewer.loadInline(viewer
							.getFileAsString(infile.getAbsolutePath()));
			viewer.evalString("select all; delay 1; move 180 0 0 0 0 0 0 0 5; delay 1; move -180 0 0 0 0 0 0 0 5;");
		} catch (Exception e) {
			
			e.printStackTrace();
		}

	}


	public void internalFrameActivated(InternalFrameEvent e) {
	}


	public void internalFrameClosed(InternalFrameEvent e) {
		openDialogCount--;
		try{
			File file = new File(fileName);
			file.delete();
		}
		catch (Exception eFile){
			eFile.printStackTrace();
		}
	}


	public void internalFrameClosing(InternalFrameEvent e) {
	}


	public void internalFrameDeactivated(InternalFrameEvent e) {
	}


	public void internalFrameDeiconified(InternalFrameEvent e) {
	}


	public void internalFrameIconified(InternalFrameEvent e) {
	}


	public void internalFrameOpened(InternalFrameEvent e) {
	}
	
}
