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

package net.sf.mzmine.modules.visualization.multimsms;

import java.text.NumberFormat;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrumDataSet;

/**
 * Label generator for spectra visualizer. Only used to generate labels for the raw data
 * (ScanDataSet)
 */
public class SpectraItemLabelGenerator implements XYItemLabelGenerator {

  /*
   * Number of screen pixels to reserve for each label, so that the labels do not overlap
   */
  public static final int POINTS_RESERVE_X = 100;

  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

  public SpectraItemLabelGenerator() {}

  /**
   * Labels for mz signals
   * 
   * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {
    // Create label
    String label = null;
    if (dataset instanceof PseudoSpectrumDataSet) {
      double mzValue = dataset.getXValue(series, item);
      label = mzFormat.format(mzValue);
      String ann = ((PseudoSpectrumDataSet) dataset).getAnnotation(item);
      if (ann != null)
        label = label + "\n" + ann;
      return label;
    }
    if (label == null) {
      double mzValue = dataset.getXValue(series, item);
      label = mzFormat.format(mzValue);
    }

    return label;

  }

}
