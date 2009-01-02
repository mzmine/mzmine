/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.pubchem.molstructureviewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;

public class JMolPanelLight extends JPanel {
	
	private JmolViewer viewer;
	JmolAdapter adapter;
	final Dimension currentSize = new Dimension();
	final Rectangle rectClip = new Rectangle();

	/**
	 * 
	 */
	public JMolPanelLight() {
		adapter = new SmarterJmolAdapter();
		viewer = JmolViewer.allocateViewer(this, adapter);
		viewer.setColorBackground("white");
		viewer.setShowHydrogens(false);
	}

	/**
	 * Returns the Jmol.viewer object
	 * 
	 * @return
	 */
	public JmolViewer getViewer() {
		return viewer;
	}
	
	/**
	 * This method help to execute a string as a script command for Jmol
	 * 
	 * @param rasmolScript
	 */
	public void executeCmd(String rasmolScript) {
		viewer.evalString(rasmolScript);
	}

	/**
	 * 
	 */
	public void paint(Graphics g) {
		getSize(currentSize);
		g.getClipBounds(rectClip);
		viewer.renderScreenImage(g, currentSize, rectClip);
	}
}