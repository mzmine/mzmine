/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.molstructure;

import java.awt.Graphics2D;
import org.jfree.fx.FXGraphics2D;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import javafx.scene.canvas.Canvas;

public class Structure3DComponent extends Canvas {


  private JmolViewer viewer;
  private JmolAdapter adapter;

  /**
   *
   */
  public Structure3DComponent() {
    adapter = new SmarterJmolAdapter();
    viewer = JmolViewer.allocateViewer(this, adapter, null, null, null, null, null);
    viewer.setColorBackground("white");
    viewer.setShowHydrogens(false);

    paint();

    widthProperty().addListener(e -> paint());
    heightProperty().addListener(e -> paint());
  }

  /**
   * Loading the structure cannot be performed in the constructor, because that would cause Jmol to
   * freeze. Therefore, we need additional method loadStructure() which is called after the
   * component is constructed.
   */
  public void loadStructure(String structure) {
    viewer.loadInline(structure);
  }


  public void paint() {

    Graphics2D g2 = new FXGraphics2D(this.getGraphicsContext2D());

    int width = (int) getWidth();
    int height = (int) getHeight();
    viewer.renderScreenImage(g2, width, height);
  }
}
