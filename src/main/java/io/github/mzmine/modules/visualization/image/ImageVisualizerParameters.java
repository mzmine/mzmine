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

package io.github.mzmine.modules.visualization.image;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageVisualizerParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter("Scan " + "selection",
          "Filter scans based on their properties. Different noise levels ( -> mass "
              + "lists) are recommended for MS1 and MS/MS scans",
          new ScanSelection());

  public static final MZRangeParameter mzRange =
      new MZRangeParameter("m/z range", "Select m/z range");

  public ImageVisualizerParameters() {
    super(new Parameter[]{rawDataFiles, scanSelection, mzRange});
  }
}
