package net.sf.mzmine.util;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;

import net.sf.mzmine.main.MZmineCore;

import org.apache.xmlgraphics.java2d.ps.EPSDocumentGraphics2D;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.JFreeChart;

public class SaveImage implements Runnable {
    
    public enum FileType {
	EMF, EPS
    };
    
    private JFreeChart chart;
    private String file;
    private int width;
    private int height;
    private final FileType fileType;
    
    public SaveImage(JFreeChart c, String f, int w, int h, FileType type) {
	chart = c;
	file = f;
	width = w;
	height = h;
	fileType = type;
    }
    
    public void run() {

	try {
	    
	    if (fileType.equals(FileType.EMF)) {
		OutputStream out2 = new java.io.FileOutputStream(file);
		EMFGraphics2D g2d2 = new EMFGraphics2D(out2,new Dimension(width,height));
		g2d2.startExport();
		chart.draw(g2d2,new Rectangle(width,height));
		g2d2.endExport();
		g2d2.closeStream();
	    }
	    
	    if (fileType.equals(FileType.EPS)) {
		OutputStream out = new java.io.FileOutputStream(file);
		EPSDocumentGraphics2D g2d = new EPSDocumentGraphics2D(false);
		g2d.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());
		g2d.setupDocument(out,width, height);
		chart.draw(g2d,new Rectangle(width,height));
		g2d.finish();
		out.flush();
		out.close();
	    }
	} catch (IOException e) {
	    MZmineCore.getDesktop().displayErrorMessage(MZmineCore.getDesktop().getMainWindow(), "Unable to save image.");
	    e.printStackTrace();
	}
    }

}
