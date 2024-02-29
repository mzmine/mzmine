/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.framework.fx.mvci.FxViewBuilder;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class PCAViewBuilder extends FxViewBuilder<PCAModel> {

  private static final int space = 5;

  private final SimpleXYChart<?> scoresPlot = new SimpleXYChart<>("Scores plot", "PC1", "PC2");
  private final SimpleXYChart<?> loadingsPlot = new SimpleXYChart<>("Loadings plot", "PC1", "PC2");

  public PCAViewBuilder(PCAModel model) {
    super(model);
  }

  @Override
  public Region build() {

    final BorderPane pane = new BorderPane();

    final Label domainLabel = new Label("Domain PC:");
    final ComboBox<Integer> domainPcSelector = new ComboBox<>(model.getAvailablePCs());
    final HBox domain = new HBox(5, domainLabel, domainPcSelector);

    final Label rangeLabel = new Label("Range PC:");
    final ComboBox<Integer> rangePcSelector = new ComboBox<>(model.getAvailablePCs());
    final HBox range = new HBox(5, rangeLabel, rangePcSelector);

    pane.setBottom(new FlowPane(space, space, domain, range));

    pane.setCenter(new HBox(scoresPlot, loadingsPlot));

    return pane;
  }
}
