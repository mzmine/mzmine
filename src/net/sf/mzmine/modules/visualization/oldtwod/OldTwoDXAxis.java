package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.Color;
import java.awt.Graphics;
import java.text.NumberFormat;

import javax.swing.JPanel;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;

public class OldTwoDXAxis extends JPanel {

	private final int leftMargin = 100;
	private final int rightMargin = 5;

	private OldTwoDDataSet dataset;

	public OldTwoDXAxis(OldTwoDDataSet dataset) {
		super();
		this.dataset = dataset;
	}

	public void paint(Graphics g) {


		super.paint(g);

		Desktop desktop = MZmineCore.getDesktop();
        NumberFormat rtFormat = desktop.getRTFormat();

        float minRT = 0;
        float maxRT = 0;
        int minScan = 0;
        int maxScan = 0;
        if (dataset!=null) {
        	minRT = dataset.getMinRT();
        	maxRT = dataset.getMaxRT();
        	
        	minScan = dataset.getMinScan();
        	maxScan = dataset.getMaxScan();
        }
        
		int w = getWidth();
		double h = getHeight();

		int pixelspertic = 50;
		int numoftics = (int) java.lang.Math.round( (float)(w-leftMargin-rightMargin) / (float)pixelspertic );
		float secspertic = (maxRT-minRT) / (float)numoftics;
		

		// Draw axis
		this.setForeground(Color.black);
		g.drawLine((int)leftMargin,0,(int)(w-rightMargin),0);

		// Draw tics and numbers
		String tmps;
		double xpos = leftMargin;
		float xval = minRT;
		for (int t=0; t<numoftics; t++) {
			// if (t==(numoftics-1)) { this.setForeground(Color.red); }

			tmps = null;
			tmps = rtFormat.format(xval); 
				
			g.drawLine((int)java.lang.Math.round(xpos), 0, (int)java.lang.Math.round(xpos), (int)(h/4));
			g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)java.lang.Math.round(xpos),(int)(3*h/4));

			xval += secspertic;
			xpos += pixelspertic;
		}

	}

}


