package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYDataset;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScaleSetupDialog;
import net.sf.mzmine.userinterface.dialogs.AxesSetupDialog;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class ProjectionPlotWindow extends JInternalFrame implements ActionListener {

	private Desktop desktop;
	private ProjectionPlotToolbar toolbar;
	private ProjectionPlotPanel plot;
	
	public ProjectionPlotWindow(Desktop desktop, ProjectionPlotDataset dataset, PeakList peakList, SimpleParameterSet parameters) {
		super(null, true, true, true, true);
		
		this.desktop = desktop;
		
        toolbar = new ProjectionPlotToolbar(this);
        add(toolbar, BorderLayout.EAST);
        
        plot = new ProjectionPlotPanel(this, dataset);
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
               
	}

}
