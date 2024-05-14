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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;

public class DisableableTreeCellRenderer extends DefaultTreeCellRenderer {

  private static Logger logger = Logger.getLogger(DisableableTreeCellRenderer.class.getName());
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public DisableableTreeCellRenderer() {
    super();
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected,
      boolean expanded, boolean leaf, int row, boolean hasFocus) {

    JComponent c = (JComponent) super.getTreeCellRendererComponent(tree, value, isSelected,
        expanded, leaf, row, hasFocus);

    if (value instanceof DisableableTreeNode)
      c.setEnabled(((DisableableTreeNode) value).isEnabled());

    return c;
  }

  // private Color getNodeColor(DPPMSLevelTreeNode node, boolean diffMSn) {
  // if(!diffMSn && node.getMSLevel() == MSLevel.MSONE)
  // return Color.GREEN;
  // if(!diffMSn && node.getMSLevel() == MSLevel.MSMS)
  // return Color.RED;
  //
  // return Color.GREEN;
  // }
}
