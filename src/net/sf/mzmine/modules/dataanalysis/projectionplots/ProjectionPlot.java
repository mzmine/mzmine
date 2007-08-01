package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class ProjectionPlot implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    public static final String MeasurementTypeArea = "Area";
    public static final String MeasurementTypeHeight = "Height";

    public static final Object[] MeasurementTypePossibleValues = {
    	MeasurementTypeArea, MeasurementTypeHeight };    
    
    public static final Parameter MeasurementType = new SimpleParameter(
            ParameterType.STRING,
            "Peak measurement type",
            "Whether peak's area or height is used in computations",
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
        
        
        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Principal component analysis (PCA)", this, "PCA_PLOT",
                KeyEvent.VK_C, false, true);
        
        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Curvilinear distance analysis (CDA)", this, "CDA_PLOT",
                KeyEvent.VK_C, false, true);        

    }

    public String toString() {
        return "Projection plot analyzer";
    }    
    
	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (SimpleParameterSet)parameterValues;
	}

	public ParameterSet getParameterSet() {
		return parameters;
	}
	

	public void actionPerformed(ActionEvent event) {
		  
		
		/*
    	ProjectionPlotSetupDialog debugSetupDialog = new ProjectionPlotSetupDialog(desktop, null, parameters); 
    	debugSetupDialog.setVisible(true);
    	*/
		
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
        	//ParameterSetupDialog setupDialog = new ParameterSetupDialog(desktop.getMainFrame(), "Please set projection plot parameters", parameters);
        	ProjectionPlotParameters parameterSet = new ProjectionPlotParameters(pl); 
        	ProjectionPlotSetupDialog setupDialog = new ProjectionPlotSetupDialog(pl, parameterSet); 

            setupDialog.setVisible(true);
            
        	if (setupDialog.getExitCode() != ExitCode.OK) {
        		logger.info("Analysis cancelled.");
        		return;
        	}
        	
        	// Create dataset & paint scale
        	ProjectionPlotDataset dataset = null;
        	/*
        	if (command.equals("PCA_PLOT"))
        		dataset = new PCADataset(pl, setupDialog.getColoringParameter(), setupDialog.getColorsForParameterValues(), parameters, 1, 2);
        	
        	
        	if (command.equals("CDA_PLOT"))
        		dataset = new CDADataset(pl, parameters);
        	*/

        	// Create & show window
        	ProjectionPlotWindow window = new ProjectionPlotWindow(desktop, dataset, pl, parameters);
        	window.setVisible(true);

        }
     
	}

}
