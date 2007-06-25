package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

import org.jfree.data.xy.AbstractXYZDataset;

public class RTMZAnalyzer implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    public static final String MeasurementTypeArea = "Area";
    public static final String MeasurementTypeHeight = "Height";

    public static final Object[] MeasurementTypePossibleValues = {
    	MeasurementTypeArea, MeasurementTypeHeight };    
    
    public static final Parameter MeasurementType = new SimpleParameter(
            ParameterType.STRING,
            "Peak measurement type",
            "Determines whether peak's area or height is used in computations.",
            MeasurementTypeArea, MeasurementTypePossibleValues);    
    

    private Desktop desktop;

    private SimpleParameterSet parameters;
	


    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new SimpleParameterSet(
                new Parameter[] { MeasurementType });
        
        
        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Coefficient of variation (CV) analysis", this, "CV_PLOT",
                KeyEvent.VK_C, false, true);
        
        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Logratio analysis", this, "LOGRATIO_PLOT",
                KeyEvent.VK_C, false, true);        

    }

    public String toString() {
        return "RT vs m/z analyzer";
    }    
    
	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (SimpleParameterSet)parameterValues;
	}

	public ParameterSet getParameterSet() {
		return parameters;
	}
	

	public void actionPerformed(ActionEvent event) {
		              
        PeakList[] alignedPeakLists = desktop.getSelectedAlignedPeakLists();
        
        if (alignedPeakLists.length==0) {
        	desktop.displayErrorMessage("Please select at least one aligned peak list.");
        }
        
        String command = event.getActionCommand();

        for (PeakList pl : alignedPeakLists) {
        	
        	if (pl.getRawDataFiles().length<2) {
        		desktop.displayErrorMessage("Alignment " + pl.toString() + " contains less than two peak lists.");
        		continue;
        	}

        	// Show opened raw data file selection and parameter setup dialog 
        	RTMZSetupDialog setupDialog = null;
            if (command.equals("CV_PLOT"))
            	setupDialog = new RTMZSetupDialog(desktop, pl.getRawDataFiles(), parameters, RTMZSetupDialog.SelectionMode.SingleGroup); 
            
            if (command.equals("LOGRATIO_PLOT"))
            	setupDialog = new RTMZSetupDialog(desktop, pl.getRawDataFiles(), parameters, RTMZSetupDialog.SelectionMode.TwoGroups);

            setupDialog.setVisible(true);
            
        	if (setupDialog.getExitCode() != ExitCode.OK) {
        		logger.info("Analysis cancelled.");
        		return;
        	}
        	
        	// Create dataset & paint scale
        	AbstractXYZDataset dataset = null;
        	InterpolatingLookupPaintScale paintScale = null;
        	if (command.equals("CV_PLOT")) {
        		dataset = new CVDataset(pl, setupDialog.getGroupOneSelectedFiles(), parameters);
        	
	    		paintScale = new InterpolatingLookupPaintScale();
	            paintScale.add(0.00, new Color(0,  0,  0));
	            paintScale.add(0.15, new Color(102,255,102));
	            paintScale.add(0.30, new Color( 51,102,255));
	            paintScale.add(0.45, new Color(255,  0,  0));
        	}
        	
        	
        	if (command.equals("LOGRATIO_PLOT")) {
        		dataset = new LogratioDataset(pl, setupDialog.getGroupOneSelectedFiles(), setupDialog.getGroupTwoSelectedFiles(), parameters);
        		
        		paintScale = new InterpolatingLookupPaintScale();
        		paintScale.add(-1.00, new Color(0,255,0));
        		paintScale.add(0.00, new Color(0,0,0));
        		paintScale.add(1.00, new Color(255,0,0));
        	}        		

        	        	
        	// Create & show window
        	RTMZAnalyzerWindow window = new RTMZAnalyzerWindow(desktop, dataset, pl, parameters, paintScale);
        	window.setVisible(true);
        	
        }
     
	}

}
