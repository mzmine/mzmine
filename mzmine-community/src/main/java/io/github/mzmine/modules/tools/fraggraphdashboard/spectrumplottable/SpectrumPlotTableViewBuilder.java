/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraItemLabelGenerator;
import java.awt.Color;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;

public class SpectrumPlotTableViewBuilder extends FxViewBuilder<SpectrumPlotTableModel> {

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
        model.getDomainMarkers().forEach(m -> plot.getXYPlot().addDomainMarker(m));
      });
    });
    plot.setMinSize(200, 200);

    initAnnotationListener(plot);
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

  private void initAnnotationListener(final SpectraPlot plot) {
    model.getDomainMarkers().addListener(
        (ListChangeListener<Marker>) change -> plot.applyWithNotifyChanges(false, () -> {
          while (change.next()) {
            if (change.wasAdded()) {
              final List<? extends Marker> added = change.getAddedSubList();
              added.forEach(a -> plot.getXYPlot().addDomainMarker(a));
            }
            if (change.wasRemoved()) {
              final List<? extends Marker> removed = change.getRemoved();
              removed.forEach(a -> plot.getXYPlot().removeDomainMarker(a));
            }
          }
        }));
  }

  public enum Layout {
    HORIZONTAL, VERTICAL, TAB;
  }
}
