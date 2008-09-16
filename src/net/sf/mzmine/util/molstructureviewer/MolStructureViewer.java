package net.sf.mzmine.util.molstructureviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.main.MZmineCore;

import org.jmol.api.JmolViewer;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.controller.Controller2DModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.HydrogenAdder;

public class MolStructureViewer extends JDialog implements WindowListener {
	
	private JMolPanelLight jmolPanel;
	private File inFile;
    private static int openDialogCount = 0;
    private static final int xOffset = 30, yOffset = 30;
	
	public MolStructureViewer(int CID, String compoundName){
		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), false);
		
		addWindowListener(this);		
		add(JChemPanelLight.getEmptyPanelWithModel());
		setPreferredSize(new Dimension(1000, 500));
		setTitle("Molecular structure visualization of CID " + CID);

		JChemPanelLight jcp = (JChemPanelLight) this.getContentPane().getComponent(0);
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
		
		pack();
		openDialogCount++;
		setLocation(xOffset*openDialogCount, yOffset*openDialogCount);
		setVisible(true);

		URL url;
		try {
			url = new URL(
					"http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="+CID+"&disopt=DisplaySDF");
			InputStream in=url.openStream ();
			
			File inFile = writeFile(in);
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
		
		
		
	}

	
	private File writeFile(InputStream inStream) throws IOException {
		File file = new File("temp.sdf");
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


	public void windowActivated(WindowEvent e) {
	}


	public void windowClosed(WindowEvent e) {
		openDialogCount--;
		try{
		inFile.delete();
		}
		catch (Exception eFile){
			eFile.printStackTrace();
		}
		dispose();
	}


	public void windowClosing(WindowEvent e) {
	}


	public void windowDeactivated(WindowEvent e) {
	}


	public void windowDeiconified(WindowEvent e) {
	}


	public void windowIconified(WindowEvent e) {
	}


	public void windowOpened(WindowEvent e) {
	}
}
