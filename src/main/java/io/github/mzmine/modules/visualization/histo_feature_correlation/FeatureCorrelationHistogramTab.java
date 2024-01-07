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

package io.github.mzmine.modules.visualization.histo_feature_correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.scan_histogram.chart.HistogramData;
import io.github.mzmine.modules.visualization.scan_histogram.chart.HistogramPanel;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;

/**
 * Histogram of Pearson correlation values for grouped features Enhanced version. Use arrows to jump
 * to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FeatureCorrelationHistogramTab extends MZmineTab {

  //private final Scene mainScene;
  private final BorderPane mainPane;
  private final ModularFeatureList flist;
  protected HistogramPanel histoUnidentified;
  protected HistogramPanel histoIonIdentities;

  // parameters
  private final HistogramData dataUnidentified;
  private final HistogramData dataIdentified;

  public FeatureCorrelationHistogramTab(ModularFeatureList flist,
      DoubleArrayList valuesUnidentified, DoubleArrayList valuesIdentified, String title,
      String xLabel, double binWidth) {
    super(title, true, false);

    this.flist = flist;
    dataUnidentified = new HistogramData(valuesUnidentified.toDoubleArray());
    dataIdentified = new HistogramData(valuesIdentified.toDoubleArray());

    mainPane = new BorderPane();
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histoUnidentified = new HistogramPanel(xLabel + " (unidentified)", dataUnidentified, binWidth);

    histoIonIdentities = new HistogramPanel(xLabel + " (connected ion identities)", dataIdentified,
        binWidth);

    setContent(mainPane);

    GridPane gridPane = new GridPane();
    gridPane.add(histoUnidentified, 0, 0);
    gridPane.add(histoIonIdentities, 1, 0);
    GridPane.setVgrow(histoUnidentified, Priority.ALWAYS);
    GridPane.setVgrow(histoIonIdentities, Priority.ALWAYS);
    GridPane.setHgrow(histoUnidentified, Priority.ALWAYS);
    GridPane.setHgrow(histoIonIdentities, Priority.ALWAYS);
    mainPane.setCenter(gridPane);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return List.of(flist);
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
