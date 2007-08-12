package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import org.jfree.chart.renderer.xy.XYItemRenderer;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.AxesSetupDialog;

public class ProjectionPlotWindow extends JInternalFrame implements ActionListener {

	private Desktop desktop;
	private ProjectionPlotToolbar toolbar;
	private ProjectionPlotPanel plot;
	
	public ProjectionPlotWindow(Desktop desktop, ProjectionPlotDataset dataset, ProjectionPlotParameters parameters) {
		super(null, true, true, true, true);
		
		this.desktop = desktop;
		
        toolbar = new ProjectionPlotToolbar(this);
        add(toolbar, BorderLayout.EAST);
        
        plot = new ProjectionPlotPanel(this, dataset);
        add(plot, BorderLayout.CENTER);
        
        String title = parameters.getSourcePeakList().toString();
        title = title.concat(" : ");
        title = title.concat(dataset.toString());
        if (parameters.getPeakMeasuringMode()==parameters.PeakAreaOption)
        	title = title.concat(" (using peak areas)");
        if (parameters.getPeakMeasuringMode()==parameters.PeakHeightOption)
        	title = title.concat(" (using peak heights)");
        this.setTitle(title);
        
        pack();


		
	}
	
	public void actionPerformed(ActionEvent event) {
		
        String command = event.getActionCommand();
        
        if (command.equals("SETUP_AXES")) {
        	AxesSetupDialog dialog = new AxesSetupDialog((Frame)desktop, plot.getChart().getXYPlot());
        	dialog.setVisible(true);
        }
       
        if (command.equals("TOGGLE_LABELS")) {
        	XYItemRenderer rend = plot.getChart().getXYPlot().getRenderer();
        	rend.setBaseItemLabelsVisible(!rend.getBaseItemLabelsVisible());
        }

	}

}
