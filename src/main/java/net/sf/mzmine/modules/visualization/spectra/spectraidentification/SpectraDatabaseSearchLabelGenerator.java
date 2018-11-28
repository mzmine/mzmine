package net.sf.mzmine.modules.visualization.spectra.spectraidentification;

/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;

/**
 * Item label generator for XYPlots adds the name of a feature in form of a label
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraDatabaseSearchLabelGenerator implements XYItemLabelGenerator {

  private String[] annotations;

  public SpectraDatabaseSearchLabelGenerator(String[] annotations, SpectraPlot plot) {
    this.annotations = annotations;
  }

  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {

    // Create label
    String label = null;
    if (dataset.getSeriesKey(1).equals("Detected compounds")) {
      label = annotations[item];
    } else {
      label = "";
    }
    return label;
  }

}
