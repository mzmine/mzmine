package net.sf.mzmine.modules.dataanalysis.logratioplot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JDialog;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.visualization.tic.TICSetupDialog;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class LogratioAnalyzer implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    public static final String MeasurementTypeArea = "Area";
    public static final String MeasurementTypeHeight = "Height";

    public static final Object[] MeasurementTypePossibleValues = {
    	MeasurementTypeArea, MeasurementTypeHeight };    
    
    public static final Parameter MeasurementType = new SimpleParameter(
            ParameterType.STRING,
            "Peak measurement type",
            "Whether peak's area or height is used in computing logratios",
            MeasurementTypeArea, MeasurementTypePossibleValues);    
    

    private TaskController taskController;
    private Desktop desktop;

    private SimpleParameterSet parameters;
	


    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        parameters = new SimpleParameterSet(
                new Parameter[] { MeasurementType });
        
        
        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Logratio between groups analysis", this, null,
                KeyEvent.VK_C, false, true);

    }

    public String toString() {
        return "Logratio analyzer";
    }    
    
	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (SimpleParameterSet)parameterValues;
	}

	public ParameterSet getParameterSet() {
		return parameters;
	}
	

	public void actionPerformed(ActionEvent arg0) {
        logger.finest("Opening a new logratio analysis setup dialog");
              
        PeakList[] alignedPeakLists = desktop.getSelectedAlignedPeakLists();
        
        if (alignedPeakLists.length==0) {
        	desktop.displayErrorMessage("Please select at least one aligned peak list.");
        }

        for (PeakList pl : alignedPeakLists) {
        	
        	if (pl.getRawDataFiles().length<2) {
        		desktop.displayErrorMessage("Alignment " + pl.toString() + " contains less than two peak lists.");
        		continue;
        	}
        	/*
        	LogratioSetupDialog setupDialog = new LogratioSetupDialog(desktop, pl.getRawDataFiles(), parameters);
        	setupDialog.setVisible(true);
        	
        	if (setupDialog.getExitCode() != ExitCode.OK) {
        		logger.info("Coefficient of variation analysis cancelled.");
        		return;
        	}
        	
        	
        	logger.info("Showing coefficient of variation analysis window.");
        	CVDataset dataset = new CVDataset(pl, setupDialog.getSelectedFiles(), parameters);
        	CVAnalyzerWindow window = new CVAnalyzerWindow(desktop, dataset, pl, parameters);
        	window.setVisible(true);
        	*/
        	
        	
        }
     
	}

}
