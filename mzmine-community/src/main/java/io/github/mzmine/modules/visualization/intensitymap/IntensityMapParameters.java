/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.intensitymap;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;

public class IntensityMapParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFile = new RawDataFilesParameter();
  public static final ComboParameter<IntensityMapDimensions> mode = new ComboParameter<>(
      "Data dimensions",
      "Imaging files always show images, other files LC-MS. Select mobility frame explicitly for ion mobility data; the frame is picked from a base peak chromatogram in the visualizer.",
      FXCollections.observableArrayList(IntensityMapDimensions.values()),
      IntensityMapDimensions.AUTOMATIC);
  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));
  public static final ComboParameter<IntensityMapDataSource> dataSource = new ComboParameter<>(
      "Data source",
      "Raw data points or the mass list of each scan. Auto uses the mass list of a scan if it has one, otherwise its raw data.",
      FXCollections.observableArrayList(IntensityMapDataSource.values()),
      IntensityMapDataSource.AUTO);
  public static final MZRangeParameter mzRange = new MZRangeParameter();

  public IntensityMapParameters() {
    super(new Parameter[]{dataFile, mode, scanSelection, dataSource, mzRange});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    // mobility frames are shown in m/z and mobility
    return IonMobilitySupport.SUPPORTED;
  }
}
