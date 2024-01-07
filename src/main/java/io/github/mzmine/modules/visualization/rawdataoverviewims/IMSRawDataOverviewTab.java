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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.parameters.ParameterSet;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public class IMSRawDataOverviewTab extends MZmineTab {

  private final IMSRawDataOverviewPane pane;

  public IMSRawDataOverviewTab() {
    super("Ion mobility raw data overview", true, false);
    pane = new IMSRawDataOverviewPane();
    setContent(pane);
  }

  public IMSRawDataOverviewTab(ParameterSet parameterSet) {
    super("Ion mobility raw data overview", true, false);
    pane = new IMSRawDataOverviewPane(
        parameterSet.getParameter(IMSRawDataOverviewParameters.summedFrameNoiseLevel)
            .getValue(),
        parameterSet.getParameter(IMSRawDataOverviewParameters.mobilityScanNoiseLevel)
            .getValue(),
        parameterSet.getParameter(IMSRawDataOverviewParameters.mzTolerance).getValue(),
        parameterSet.getParameter(IMSRawDataOverviewParameters.scanSelection).getValue(),
        parameterSet.getParameter(IMSRawDataOverviewParameters.rtWidth).getValue().floatValue(),
        parameterSet.getParameter(IMSRawDataOverviewParameters.binWidth).getValue());
    setContent(pane);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptySet();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptySet();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptySet();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    pane.setRawDataFile(rawDataFiles.stream().findFirst().get());
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
