package net.sf.mzmine.modules.identification.pubchem.molstructureviewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;

public class JMolPanelLight extends JPanel {
	/**
     * 
     */
	JmolViewer viewer;
	JmolAdapter adapter;

	public JMolPanelLight() {
		adapter = new SmarterJmolAdapter();
		viewer = JmolViewer.allocateViewer(this, adapter);
		viewer.setColorBackground("white");
		viewer.setShowHydrogens(false);
	}

	public JmolViewer getViewer() {
		return viewer;
	}

	public void executeCmd(String rasmolScript) {
		viewer.evalString(rasmolScript);
	}

	final Dimension currentSize = new Dimension();
	final Rectangle rectClip = new Rectangle();

	public void paint(Graphics g) {
		getSize(currentSize);
		g.getClipBounds(rectClip);
		viewer.renderScreenImage(g, currentSize, rectClip);
	}
}