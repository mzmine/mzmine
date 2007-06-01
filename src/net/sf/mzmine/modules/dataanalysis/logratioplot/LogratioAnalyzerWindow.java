package net.sf.mzmine.modules.dataanalysis.logratioplot;

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

public class LogratioAnalyzerWindow extends JInternalFrame implements ActionListener {

	private Desktop desktop;
	private LogratioToolbar toolbar;
	private LogratioPlot plot;
	
	public LogratioAnalyzerWindow(Desktop desktop, LogratioDataset dataset, PeakList peakList, SimpleParameterSet parameters) {
		super(null, true, true, true, true);
		
		this.desktop = desktop;
		
        toolbar = new LogratioToolbar(this);
        add(toolbar, BorderLayout.EAST);
        
        plot = new LogratioPlot(this, dataset);
        add(plot, BorderLayout.CENTER);
        
        String title = peakList.toString();
        title = title.concat(" : Logratio ");
        if (parameters.getParameterValue(LogratioAnalyzer.MeasurementType)==LogratioAnalyzer.MeasurementTypeArea)
        	title = title.concat("(peak area)");
        if (parameters.getParameterValue(LogratioAnalyzer.MeasurementType)==LogratioAnalyzer.MeasurementTypeHeight)
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
