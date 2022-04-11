/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.molstructure;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import javafx.embed.swing.SwingNode;

public class Structure3DComponent extends SwingNode {

  private final JPanel mainPanel;
  private JmolViewer viewer;
  private JmolAdapter adapter;
  final Dimension currentSize = new Dimension();
  final Rectangle rectClip = new Rectangle();

  /**
   *
   */
  public Structure3DComponent() {

    mainPanel = new JPanel() {
      @Override
      public void paint(Graphics g) {
        mainPanel.getSize(currentSize);
        g.getClipBounds(rectClip);
        viewer.renderScreenImage(g, currentSize, rectClip);
      }
    };

    adapter = new SmarterJmolAdapter();
    viewer = JmolViewer.allocateViewer(mainPanel, adapter, null, null, null, null, null);
    viewer.setColorBackground("white");
    viewer.setShowHydrogens(false);

    SwingUtilities.invokeLater(() -> setContent(mainPanel));
  }

  /**
   * Loading the structure cannot be performed in the constructor, because that would cause Jmol to
   * freeze. Therefore, we need additional method loadStructure() which is called after the
   * component is constructed.
   */
  public void loadStructure(String structure) {
    viewer.loadInline(structure);
  }


}
