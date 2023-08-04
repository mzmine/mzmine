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

package io.github.mzmine.modules.visualization.massvoltammogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramPlotPanel;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramToolBar;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import java.util.Collection;
import java.util.List;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTab extends MZmineTab {

  public MassvoltammogramTab(String title, Massvoltammogram massvoltammogram) {
    super(title);

    //Creating a pane to add the nodes to.
    final BorderPane mainPane = new BorderPane();

    //Converting the plot from a swing panel to a javafx node.
    final MassvoltammogramPlotPanel plot = massvoltammogram.getPlot();
    final SwingNode swingNodePlot = new SwingNode();
    swingNodePlot.setContent(plot);

    //Creating a label from the filename to identify the massvoltammogram.
    Label fileNameLable = new Label(massvoltammogram.getFileName());
    fileNameLable.setStyle("-fx-background-color: white;");
    fileNameLable.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

    //Getting the toolbar for the plot.
    final MassvoltammogramToolBar toolBar = plot.getMassvoltammogramToolBar();

    //Adding the toolbar, plot and label to the main pane.
    mainPane.setCenter(swingNodePlot);
    mainPane.setRight(toolBar);
    mainPane.setBottom(fileNameLable);

    //Setting the pane as the MassvoltammogramTabs content.
    setContent(mainPane);
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
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
