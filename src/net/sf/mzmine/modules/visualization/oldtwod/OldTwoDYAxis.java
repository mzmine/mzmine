package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.Color;
import java.awt.Graphics;
import java.text.NumberFormat;

import javax.swing.JPanel;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;

public class OldTwoDYAxis extends JPanel {

	private final double bottomMargin = (double)0.0;
	private final double topMargin = (double)0.0;

	private int numTics;
	
	private OldTwoDDataSet dataset;

	public OldTwoDYAxis(OldTwoDDataSet dataset) {
		super();
		this.dataset = dataset;
	}

	public void paint(Graphics g) {

		super.paint(g);
		
		double minY = 0.0;
		double maxY = 0.0;
		if (dataset!=null) {
			minY = dataset.getMinMZ();
			maxY = dataset.getMaxMZ();
		}	

		Desktop desktop = MZmineCore.getDesktop();
        NumberFormat mzFormat = MZmineCore.getMZFormat();

		double w = getWidth();
		double h = getHeight();

		numTics = 5;
		if (h>250) { numTics = 10; }
		if (h>500) { numTics = 20; }
		if (h>1000) { numTics = 40; }

		this.setForeground(Color.black);
		g.drawLine((int)w-1,0,(int)w-1,(int)h);

		String tmps;

		double diff_dat = maxY-minY;
		double diff_scr = h - bottomMargin - topMargin;
		double ypos = bottomMargin;
		double yval = minY;
		for (int t=1; t<=numTics; t++) {

			tmps = mzFormat.format(yval);
			g.drawLine((int)(3*w/4), (int)(h-ypos), (int)(w), (int)(h-ypos));
			g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)(w/4)-4,(int)(h-ypos));

			yval += diff_dat / numTics;
			ypos += diff_scr / numTics;
		}

	}

}

