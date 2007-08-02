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
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotDialog;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotFrame;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotParameters;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class ProjectionPlot implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
/*
    public static final String MeasurementTypeArea = "Area";
    public static final String MeasurementTypeHeight = "Height";

    public static final Object[] MeasurementTypePossibleValues = {
    	MeasurementTypeArea, MeasurementTypeHeight };    
    
    public static final Parameter MeasurementType = new SimpleParameter(
            ParameterType.STRING,
            "Peak measurement type",
            "Whether peak's area or height is used in computations",
            MeasurementTypeArea, MeasurementTypePossibleValues);    
*/    

    private Desktop desktop;

    private ProjectionPlotParameters parameters;
	


    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();       
        
        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Principal component analysis (PCA)", this, "PCA_PLOT",
                KeyEvent.VK_C, false, true);
        
        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Curvilinear distance analysis (CDA)", this, "CDA_PLOT",
                KeyEvent.VK_C, false, true);        

    }

    public String toString() {
        return "Projection plot analyzer";
    }    
    
	public void setParameters(ParameterSet parameters) {
		this.parameters = (ProjectionPlotParameters)parameters;
	}

	public ProjectionPlotParameters getParameterSet() {
		return parameters;
	}
	

	public void actionPerformed(ActionEvent event) {
	
		
        PeakList selectedAlignedPeakLists[] = desktop.getSelectedAlignedPeakLists();
        if (selectedAlignedPeakLists.length != 1) {
            desktop.displayErrorMessage("Please select a single aligned peaklist");
            return;
        }

        if (selectedAlignedPeakLists[0].getNumberOfRows() == 0) {
            desktop.displayErrorMessage("Selected alignment result is empty");
            return;
        }

        logger.finest("Showing projection plot setup dialog");

        if ((parameters==null) || (selectedAlignedPeakLists[0] != parameters.getSourcePeakList())) {
            parameters = new ProjectionPlotParameters(
                    selectedAlignedPeakLists[0]);
        }

        ProjectionPlotSetupDialog setupDialog = new ProjectionPlotSetupDialog(
                selectedAlignedPeakLists[0], parameters);
        setupDialog.setVisible(true);

        if (setupDialog.getExitCode() == ExitCode.OK) {
            logger.info("Opening new projection plot");
            
            ProjectionPlotDataset dataset = null;
            
            String command = event.getActionCommand();
        	if (command.equals("PCA_PLOT"))
        		dataset = new PCADataset(parameters, 1, 2);
        	
        	/*
        	if (command.equals("CDA_PLOT"))
        		dataset = new CDADataset();
        	*/
        	          
        	ProjectionPlotWindow newFrame = new ProjectionPlotWindow(desktop, dataset, parameters);       	
            desktop.addInternalFrame(newFrame);
            
        }
		
		
     
	}

}
