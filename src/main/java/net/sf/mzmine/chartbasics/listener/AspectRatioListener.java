/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.chartbasics.listener;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JPanel;
import org.jfree.chart.ChartPanel;
import net.sf.mzmine.chartbasics.ChartLogics;

/**
 * override component Resize test for resize and call resize fucntion
 * 
 * @author
 */
public abstract class AspectRatioListener implements ComponentListener {
  public enum RATIO {
    LIMIT_TO_PN_WIDTH, LIMIT_TO_PARENT_SIZE
  }

  private boolean keepRatio = true;
  private RATIO ratio;
  private JPanel parent;

  public AspectRatioListener(JPanel parent, boolean keepRatio, RATIO ratio) {
    super();
    this.keepRatio = keepRatio;
    this.ratio = ratio;
    this.parent = parent;
  }


  /**
   * resize pn to parent with ratio
   * 
   * @param pn
   * @param parent
   */
  public void resize(ChartPanel pn, RATIO ratio) {
    if (keepRatio) {
      Dimension dim = null;
      switch (ratio) {
        case LIMIT_TO_PARENT_SIZE:
          dim = ChartLogics.calcMaxSize(pn, parent.getWidth(), parent.getHeight());
          // pnChartAspectRatio.setSize(dim);
          break;
        case LIMIT_TO_PN_WIDTH:
          int height = (int) ChartLogics.calcHeightToWidth(pn, pn.getWidth(), true);
          dim = new Dimension(50, height);
          break;
      }
      pn.setPreferredSize(dim);
      pn.setSize(dim);
      parent.revalidate();
      parent.repaint();
    }
  }


  /**
   * resize pn to parent with ratio
   * 
   * @param pn
   * @param parent
   */
  public void resize(ChartPanel pn) {
    resize(pn, ratio);
  }

  @Override
  public void componentMoved(ComponentEvent e) {}

  @Override
  public void componentShown(ComponentEvent e) {}

  @Override
  public void componentHidden(ComponentEvent e) {}

  public boolean isKeepRatio() {
    return keepRatio;
  }

  public void setKeepRatio(boolean keepRatio) {
    this.keepRatio = keepRatio;
  }

  public JPanel getParent() {
    return parent;
  }

  public void setParent(JPanel parent) {
    this.parent = parent;
  }

  public RATIO getRatio() {
    return ratio;
  }

  public void setRatio(RATIO ratio) {
    this.ratio = ratio;
  }
}
