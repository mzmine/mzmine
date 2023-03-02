/*
 * Copyright 2006-2022 The MZmine Development Team
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
 */

package io.github.mzmine.modules.visualization.massvoltammogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.ExtendedPlot3DPanel;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramToolBar;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import java.util.Collection;
import java.util.List;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTab extends MZmineTab {

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

  public MassvoltammogramTab(String title, Massvoltammogram massvoltammogram) {
    super(title);

    //Creating a pane to add the nodes to.
    final BorderPane mainPane = new BorderPane();

    //Converting the plot from a swing panel to a javafx node.
    final ExtendedPlot3DPanel plot = massvoltammogram.getPlot();
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
}
