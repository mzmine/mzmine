/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.image;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleBoundStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleColorStyle;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.PaintScaleParameter;
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

  public static final PaintScaleParameter paintScale =
      new PaintScaleParameter("Paint scale", "Select paint scale",
          new PaintScale[] {
              new PaintScale(PaintScaleColorStyle.RAINBOW, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.RED, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.GREEN, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.CYAN, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.YELLOW, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0))});

  public ImageVisualizerParameters() {
    super(new Parameter[] {rawDataFiles, scanSelection, mzRange, paintScale});
  }
}
