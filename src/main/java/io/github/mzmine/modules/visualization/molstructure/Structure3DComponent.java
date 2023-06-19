/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
