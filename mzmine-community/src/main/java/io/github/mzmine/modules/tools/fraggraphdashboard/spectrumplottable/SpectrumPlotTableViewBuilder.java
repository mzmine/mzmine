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
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ProviderAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import java.util.logging.Logger;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import org.freehep.graphicsio.emf.gdi.TextA;

public class SpectrumPlotTableViewBuilder extends FxViewBuilder<SpectrumPlotTableModel> {

  private static Logger logger = Logger.getLogger(SpectrumPlotTableViewBuilder.class.getName());

  private final Layout layout;

  SpectrumPlotTableViewBuilder(SpectrumPlotTableModel model, Layout layout) {
    super(model);
    this.layout = layout;
  }

  @Override
  public Region build() {
    Region pane;
    TextArea peakTable = new TextArea();
    final NumberFormats formats = ConfigService.getGuiFormats();
    model.signalListProperty().bindBidirectional(peakTable.textProperty());
    peakTable.textProperty().addListener((_, _, t) -> model.signalListProperty()
        .getValue()); // if we dont do this the binding is only updated when it feels like it
//    model.signalListProperty().addListener((_, _, t) -> logger.info("trigger signalList"));
//    model.spectrumProperty().addListener((_, _, t) -> logger.info("trigger spectrum"));

    SimpleXYChart<PlotXYDataProvider> plot = new SimpleXYChart<>();
    plot.setDomainAxisLabel("m/z");
    plot.setDomainAxisNumberFormatOverride(formats.mzFormat());
    plot.setRangeAxisLabel("Intensity");
    plot.setRangeAxisNumberFormatOverride(formats.intensityFormat());

    model.spectrumProperty().addListener((_, _, spec) -> {
      plot.applyWithNotifyChanges(false, () -> {
        plot.removeAllDatasets();
        if (spec == null) {
          return;
        }
        plot.addDataset(new ProviderAndRenderer(new MassSpectrumProvider(spec, "Spectrum",
            ConfigService.getDefaultColorPalette().getAWT(0)), new ColoredXYBarRenderer(false)));
      });
    });
    plot.setMinSize(200, 200);

    pane = switch (layout) {
      case HORIZONTAL, VERTICAL -> {
        var split = new SplitPane(plot, peakTable);
        split.setOrientation(
            layout == Layout.HORIZONTAL ? Orientation.HORIZONTAL : Orientation.VERTICAL);
        if(layout == Layout.HORIZONTAL) {
          split.setDividerPositions(0.65);
        }
        yield split;
      }
      case TAB -> {
        var spectrumTab = new Tab("Spectrum");
        spectrumTab.setContent(plot);
        var textTab = new Tab("Signal list");
        textTab.setContent(peakTable);
        yield new TabPane(spectrumTab, textTab);
      }
    };
    return pane;
  }

  public enum Layout {
    HORIZONTAL, VERTICAL, TAB;
  }
}
