/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.LipidSpectrumProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;

public class LipidSpectrumChart extends StackPane {

  public LipidSpectrumChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {

    SpectraPlot spectraPlot = new SpectraPlot();
    spectraPlot.setPrefHeight(GraphicalColumType.DEFAULT_IMAGE_CELL_HEIGHT);
    List<MatchedLipid> matchedLipids = row.get(LipidMatchListType.class);
    if (matchedLipids != null && !matchedLipids.isEmpty()) {
      MatchedLipid match = matchedLipids.get(0);
      if (match.getMatchedFragments() != null && !match.getMatchedFragments().isEmpty()) {
        List<LipidFragment> matchedFragments = new ArrayList<>(match.getMatchedFragments());
        Scan matchedMsMsScan =
            matchedFragments.stream().map(LipidFragment::getMsMsScan).findFirst().orElse(null);
        if (matchedMsMsScan != null) {
          PlotXYDataProvider spectrumProvider =
              new LipidSpectrumProvider(null, matchedMsMsScan, "MS/MS Spectrum",
                  MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT());
          ColoredXYDataset spectrumDataSet = new ColoredXYDataset(spectrumProvider);
          spectraPlot.addDataSet(spectrumDataSet,
              MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT(), true,
              null);
        }

        List<DataPoint> fragmentScanDps =
            matchedFragments.stream().map(LipidFragment::getDataPoint).collect(Collectors.toList());
        if (!fragmentScanDps.isEmpty()) {
          PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(matchedFragments,
              fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
              fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
              "Matched Signals",
              MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
          ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider);
          MatchedLipidLabelGenerator matchedLipidLabelGenerator =
              new MatchedLipidLabelGenerator(spectraPlot, matchedFragments);
          spectraPlot.getXYPlot().getRenderer().setDefaultItemLabelsVisible(true);
          spectraPlot.getXYPlot().getRenderer().setSeriesItemLabelGenerator(
              1, matchedLipidLabelGenerator);
          spectraPlot.addDataSet(fragmentDataSet,
              MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT(), true,
              matchedLipidLabelGenerator);
        }
      }
      MZmineCore.runLater(() -> {
        getChildren().add(spectraPlot);
      });
    }

  }


}
