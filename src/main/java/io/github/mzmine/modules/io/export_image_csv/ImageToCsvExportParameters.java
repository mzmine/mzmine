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
