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

package net.sf.mzmine.modules.visualization.mzhistogram.chart;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;;

public class HistogramDialog extends JFrame {

  protected HistogramPanel histo;

  /**
   * Create the dialog. Bin width is automatically chosen
   */
  public HistogramDialog(String title, String xLabel, HistogramData data) {
    this(title, xLabel, data, 0);
  }

  /**
   * 
   * @param title
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public HistogramDialog(String title, String xLabel, HistogramData data, double binWidth) {
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setTitle(title);
    setBounds(100, 100, 1000, 800);
    getContentPane().setLayout(new BorderLayout());
    histo = new HistogramPanel(xLabel, data, binWidth);
    getContentPane().add(histo, BorderLayout.CENTER);
    this.setTitle(title);
  }

  public HistogramPanel getHistoPanel() {
    return histo;
  }
}
