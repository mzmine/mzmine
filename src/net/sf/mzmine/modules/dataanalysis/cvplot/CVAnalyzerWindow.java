package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScaleSetupDialog;
import net.sf.mzmine.userinterface.dialogs.AxesSetupDialog;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class CVAnalyzerWindow extends JInternalFrame implements ActionListener {

	private Desktop desktop;
	private CVToolbar toolbar;
	private CVPlot plot;
	
	public CVAnalyzerWindow(Desktop desktop, CVDataset dataset, PeakList peakList, SimpleParameterSet parameters) {
		super(null, true, true, true, true);
		
		this.desktop = desktop;
		
        toolbar = new CVToolbar(this);
        add(toolbar, BorderLayout.EAST);
        
        plot = new CVPlot(this, dataset);
        add(plot, BorderLayout.CENTER);
        
        String title = peakList.toString();
        title = title.concat(" : Coefficient of variation ");
        if (parameters.getParameterValue(CVAnalyzer.MeasurementType)==CVAnalyzer.MeasurementTypeArea)
        	title = title.concat("(peak area)");
        if (parameters.getParameterValue(CVAnalyzer.MeasurementType)==CVAnalyzer.MeasurementTypeHeight)
        	title = title.concat("(peak height)");        
        this.setTitle(title);
        
        pack();

        desktop.addInternalFrame(this);
		
	}
	
	public void actionPerformed(ActionEvent event) {
		
        String command = event.getActionCommand();
        
        if (command.equals("SETUP_AXES")) {
        	AxesSetupDialog dialog = new AxesSetupDialog((Frame)desktop, plot.getChart().getXYPlot());
        	dialog.setVisible(true);
        }
        
        if (command.equals("SETUP_COLORS")) {
        	InterpolatingLookupPaintScaleSetupDialog colorDialog = new InterpolatingLookupPaintScaleSetupDialog((Frame)desktop, plot.getPaintScale());
            colorDialog.setVisible(true);
            	
        	if (colorDialog.getExitCode()==ExitCode.OK)
        		plot.setPaintScale(colorDialog.getPaintScale());
        }
        
	}

}
