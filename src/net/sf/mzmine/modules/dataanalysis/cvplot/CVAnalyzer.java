package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JDialog;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.visualization.tic.TICSetupDialog;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class CVAnalyzer implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;
	
	
	public ParameterSet getParameterSet() {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Coefficient of variation (CV) analysis", this, null,
                KeyEvent.VK_C, false, true);

    }

    public String toString() {
        return "CV analyzer";
    }    
    
	public void setParameters(ParameterSet parameterValues) {
		// TODO Auto-generated method stub

	}

	public void actionPerformed(ActionEvent arg0) {
        logger.finest("Opening a new CV analysis setup dialog");
        
        PeakList[] alignedPeakLists = desktop.getSelectedAlignedPeakLists();
        
        if (alignedPeakLists.length==0) {
        	desktop.displayErrorMessage("Please select at least one aligned peak list.");
        }

        for (PeakList pl : alignedPeakLists) {
        	
        	if (pl.getRawDataFiles().length<2) {
        		desktop.displayErrorMessage("Alignment " + pl.toString() + " contains less than two peak lists.");
        		continue;
        	}
        	
        	CVSetupDialog setupDialog = new CVSetupDialog(desktop, pl.getRawDataFiles());
        	setupDialog.setVisible(true);
        	
        	if (setupDialog.getExitCode() == ExitCode.CANCEL) {
        		logger.info("Coefficient of variation analysis cancelled.");
        		return;
        	}
        	
        	
        	logger.info("Showing coefficient of variation analysis window.");
        	CVDataset dataset = new CVDataset(pl, setupDialog.getSelectedFiles());
        	CVAnalyzerWindow window = new CVAnalyzerWindow(desktop, dataset, pl);
        	window.setVisible(true);
        	
        	
        }
     
	}
	
	

}
