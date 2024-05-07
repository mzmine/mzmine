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
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ProviderAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraItemLabelGenerator;
import java.awt.Color;
import java.util.logging.Logger;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import org.freehep.graphicsio.emf.gdi.TextA;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;

public class SpectrumPlotTableViewBuilder extends FxViewBuilder<SpectrumPlotTableModel> {

  private static Logger logger = Logger.getLogger(SpectrumPlotTableViewBuilder.class.getName());

  private final Layout layout;

  SpectrumPlotTableViewBuilder(SpectrumPlotTableModel model, Layout layout) {
    super(model);
    this.layout = layout;
  }

  @Override
  public Region build() {
    TextArea peakTable = new TextArea();
    final NumberFormats formats = ConfigService.getGuiFormats();
    model.signalListProperty().bindBidirectional(peakTable.textProperty());
    peakTable.textProperty().addListener((_, _, t) -> model.signalListProperty()
        .getValue()); // if we dont do this the binding is only updated when it feels like it
//    model.signalListProperty().addListener((_, _, t) -> logger.info("trigger signalList"));
//    model.spectrumProperty().addListener((_, _, t) -> logger.info("trigger spectrum"));

    SpectraPlot plot = new SpectraPlot();
    plot.getXYPlot().getDomainAxis().setLabel("m/z");
    ((NumberAxis) plot.getXYPlot().getDomainAxis()).setNumberFormatOverride(formats.mzFormat());
    plot.getXYPlot().getRangeAxis().setLabel("Intensity");
    ((NumberAxis) plot.getXYPlot().getRangeAxis()).setNumberFormatOverride(
        formats.intensityFormat());

    model.spectrumProperty().addListener((_, _, spec) -> {
      plot.applyWithNotifyChanges(false, () -> {
        plot.removeAllDataSets();
        if (spec == null) {
          return;
        }
        final ColoredXYBarRenderer renderer = new ColoredXYBarRenderer(false);
        renderer.setDefaultItemLabelGenerator(new SpectraItemLabelGenerator(plot));

        plot.addDataSet(new ColoredXYDataset(new MassSpectrumProvider(spec, "Spectrum",
                ConfigService.getDefaultColorPalette().getAWT(0))), Color.black, false, renderer, false,
            false);
      });
    });
    plot.setMinSize(200, 200);

    return initialisePane(plot, peakTable);
  }

  @NotNull
  private Region initialisePane(SpectraPlot plot, TextArea peakTable) {
    return switch (layout) {
      case HORIZONTAL, VERTICAL -> {
        var split = new SplitPane(plot, peakTable);
        split.setOrientation(
            layout == Layout.HORIZONTAL ? Orientation.HORIZONTAL : Orientation.VERTICAL);
        if (layout == Layout.HORIZONTAL) {
          split.setDividerPositions(0.65);
          peakTable.setMaxWidth(200);
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
  }

  public enum Layout {
    HORIZONTAL, VERTICAL, TAB;
  }
}
