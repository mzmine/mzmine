/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.molstructure;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;

public class Structure3DComponent extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JmolViewer viewer;
    private JmolAdapter adapter;
    final Dimension currentSize = new Dimension();
    final Rectangle rectClip = new Rectangle();

    /**
	 * 
	 */
    public Structure3DComponent() {
	adapter = new SmarterJmolAdapter();
	viewer = JmolViewer.allocateViewer(this, adapter, null, null, null,
		null, null);
	viewer.setColorBackground("white");
	viewer.setShowHydrogens(false);
    }

    /**
     * Loading the structure cannot be performed in the constructor, because
     * that would cause Jmol to freeze. Therefore, we need additional method
     * loadStructure() which is called after the component is constructed.
     */
    public void loadStructure(String structure) {
	viewer.loadInline(structure);
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