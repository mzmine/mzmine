package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import org.jfree.data.xy.AbstractXYZDataset;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScaleSetupDialog;
import net.sf.mzmine.userinterface.dialogs.AxesSetupDialog;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class RTMZAnalyzerWindow extends JInternalFrame implements ActionListener {

	private Desktop desktop;
	private RTMZToolbar toolbar;
	private RTMZPlot plot;
	
	public RTMZAnalyzerWindow(Desktop desktop, AbstractXYZDataset dataset, PeakList peakList, SimpleParameterSet parameters, InterpolatingLookupPaintScale paintScale) {
		super(null, true, true, true, true);
		
		this.desktop = desktop;
		
        toolbar = new RTMZToolbar(this);
        add(toolbar, BorderLayout.EAST);
        
        plot = new RTMZPlot(this, dataset, paintScale);
        add(plot, BorderLayout.CENTER);
        
        String title = peakList.toString();
        title = title.concat(" : ");
        title = title.concat(dataset.toString());
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
