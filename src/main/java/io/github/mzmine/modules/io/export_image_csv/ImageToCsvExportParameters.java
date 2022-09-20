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

package io.github.mzmine.modules.io.export_image_csv;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;

public class ImageToCsvExportParameters extends SimpleParameterSet {

  public static final DirectoryParameter dir = new DirectoryParameter("Export directory",
      "The directory to save the files in.");
  public static final StringParameter delimiter = new StringParameter("Delimiter", "The delimiter.",
      ",");

  public static final BooleanParameter normalize = new BooleanParameter("Normalize to average TIC",
          """
          If selected, the intensities will be normalized to the average TIC of the whole raw data file.
          Example: NormalizedIntensity = Intensity_pixel / TIC_pixel * AvgTIC_file 
          """, false);

  public static final ComboParameter<HandleMissingValues> handleMissingSpectra = new ComboParameter<>(
      "Handle missing scan at x,y",
      "There might be no scan at an x,y coordinate due to irregular shapes during imaging "
          + "acquisition. Select option to handle these cases.\nDefault: leave empty",
      HandleMissingValues.values(), HandleMissingValues.LEAVE_EMPTY);

  public static final ComboParameter<HandleMissingValues> handleMissingSignals = new ComboParameter<>(
      "Handle missing signals in scans",
      "Options to report the intensity for signals that are missing in specific scans.\n"
          + "Default: replace by zero", HandleMissingValues.values(),
      HandleMissingValues.REPLACE_BY_ZERO);

  public ImageToCsvExportParameters() {
    super(new Parameter[]{dir, delimiter, normalize, handleMissingSpectra, handleMissingSignals});
  }

  /**
   * Options to handle missing values due to irregular shapes during image acquisition (no scan at
   * specific x,y coordinate) and missing signals in available scans.
   */
  public enum HandleMissingValues {
    /**
     * leave empty in csv means ,,
     */
    LEAVE_EMPTY,
    /**
     * replace by zero
     */
    REPLACE_BY_ZERO,
    /**
     * replace by lowest value in image
     */
    REPLACE_BY_LOWEST_VALUE;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }
}
