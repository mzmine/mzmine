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
 *
 *
 */

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

public class SpectrumPlotTableViewBuilder extends FxViewBuilder<SpectrumPlotTableModel> {

  private final Layout layout;

  SpectrumPlotTableViewBuilder(SpectrumPlotTableModel model, Layout layout) {
    super(model);
    this.layout = layout;
  }

  @Override
  public Region build() {

    Region pane;

    TextField peaks = new TextField();
    peaks.textProperty().bindBidirectional(model.signalListProperty());

    SimpleXYChart<PlotXYDataProvider> plot = new SimpleXYChart<>();
    model.spectrumProperty().addListener((_, _, spec) -> {
      plot.applyWithNotifyChanges(false, () -> {
        plot.removeAllDatasets();
        if (spec == null) {
          return;
        }
        plot.addDataset(new MassSpectrumProvider(spec, "Spectrum",
            ConfigService.getDefaultColorPalette().getAWT(0)));
      });
    });
    plot.setMinSize(200, 200);

    pane = switch (layout) {
      case HORIZONTAL, VERTICAL -> {
        var split = new SplitPane(plot, peaks);
        split.setOrientation(
            layout == Layout.HORIZONTAL ? Orientation.HORIZONTAL : Orientation.VERTICAL);
        yield split;
      }
      case TAB -> {
        var spectrumTab = new Tab("Spectrum");
        spectrumTab.setContent(plot);
        var textTab = new Tab("Signal list");
        textTab.setContent(peaks);
        yield new TabPane(spectrumTab, textTab);
      }
    };
    return pane;
  }

  public enum Layout {
    HORIZONTAL, VERTICAL, TAB;
  }
}
