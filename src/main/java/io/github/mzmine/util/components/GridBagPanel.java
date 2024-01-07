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

package io.github.mzmine.util.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * Simple JPanel with a GridBagLayout for easier use of the grid bag constraints. It automatically
 * adds a 5px border to the components.
 * 
 */
public class GridBagPanel extends JPanel {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private GridBagLayout layout;

  private final Insets borderInsets = new Insets(0, 0, 5, 5);

  public GridBagPanel() {
    layout = new GridBagLayout();
    setLayout(layout);
  }

  /**
   * Adds a component to the given cell (gridx:gridy) of the grid. The weight of the component is
   * set to 0, therefore it is not resized even when there is extra space available.
   */
  public void add(Component component, int gridx, int gridy) {
    add(component, gridx, gridy, 1, 1, 0, 0, GridBagConstraints.NONE);
  }

  /**
   * Adds a component to the given cell (gridx:gridy) of the grid. The width and height of the cell
   * are also specified, therefore this cell may span over several other cells. The weight of the
   * component is set to 0, therefore it is not resized even when there is extra space available.
   */
  public void add(Component component, int gridx, int gridy, int gridwidth, int gridheight) {
    add(component, gridx, gridy, gridwidth, gridheight, 0, 0, GridBagConstraints.NONE);
  }

  /**
   * Adds a component to the given cell (gridx:gridy) of the grid, with given width and height and
   * also weight for resizing.
   */
  public void add(Component component, int gridx, int gridy, int gridwidth, int gridheight,
      int weightx, int weighty) {
    add(component, gridx, gridy, gridwidth, gridheight, weightx, weighty, GridBagConstraints.NONE);
  }

  /**
   * Adds a component to the given cell (gridx:gridy) of the grid, with given width and height and
   * also weight for resizing.
   */
  public void add(Component component, int gridx, int gridy, int gridwidth, int gridheight,
      int weightx, int weighty, int fill) {

    GridBagConstraints constraints = new GridBagConstraints(gridx, gridy, gridwidth, gridheight,
        weightx, weighty, GridBagConstraints.WEST, fill, borderInsets, 0, 0);

    super.add(component);

    layout.setConstraints(component, constraints);

  }

  public void addCenter(Component component, int gridx, int gridy, int gridwidth, int gridheight) {

    GridBagConstraints constraints = new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 0,
        0, GridBagConstraints.CENTER, GridBagConstraints.NONE, borderInsets, 0, 0);

    super.add(component);

    layout.setConstraints(component, constraints);
  }

  public void addSeparator(int gridx, int gridy, int width) {
    JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
    sep.setPreferredSize(new Dimension(2, 1));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gridx;
    gbc.gridy = gridy;
    gbc.gridwidth = width;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;

    add(sep, gbc);

  }

}
