/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_sortannotations;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CombinedScoreWeightsComponent extends FlowPane {

  private final NumberFormat numberFormat = new DecimalFormat("0.0");
  private final @NotNull Property<Double> mz;
  private final @NotNull Property<Double> ms2;
  private final @NotNull Property<Double> rt;
  private final @NotNull Property<Double> ri;
  private final @NotNull Property<Double> isotopes;
  private final @NotNull Property<Double> ccs;

  public CombinedScoreWeightsComponent(@Nullable CombinedScoreWeights value) {
    super(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);

    String scoreCalc = "Distance-based score (0-1): 1 − absoluteValue((measured − reference) / max distance)";

    List<Region> weightComponents = new ArrayList<>();
    ms2 = addTextField(weightComponents, "MS2",
        "MS2 weights are multiplied with the MS2 score if available.");
    isotopes = addTextField(weightComponents, "IP", "Isotope pattern weights are multiplied with the isotope pattern scores if available.");
    mz = addTextField(weightComponents, "m/z", """
        m/z values are transformed using an m/z tolerance as maximum distance.
        """ + scoreCalc);
    rt = addTextField(weightComponents, "RT",
        "Retention time weight are multiplied with the RT score.\n" + scoreCalc);
    ri = addTextField(weightComponents, "RI", "Retention index weight are multiplied with the RI score.\n" + scoreCalc);
    ccs = addTextField(weightComponents, "CCS", "CCS weights are multiplied with the CCS score which is based on relative distances as %%.\n" + scoreCalc);

    final ObservableList<Node> children = getChildren();
    for (int i = 0; i < weightComponents.size(); i++) {
      children.add(weightComponents.get(i));
      if(i<weightComponents.size()-1){
        // + symbol to show that those are added up
        children.add(FxLabels.newLabel(Styles.BOLD_SEMI_TITLE, "+"));
      }
    }

    setValue(value);
  }

  @NotNull
  private Property<Double> addTextField(List<Region> weightComponents, @NotNull String name,
      @NotNull String tooltipText) {
    final DoubleComponent component = new DoubleComponent(45, 0d, null, numberFormat, 1d);
    final Tooltip tooltip = new Tooltip(tooltipText);
    final Label label = FxLabels.newBoldLabel(name);
    label.setTooltip(tooltip);
    final Label xLabel = FxLabels.newBoldLabel("×");
    label.setTooltip(tooltip);

    final TextField textField = component.getTextField();
    textField.setTooltip(tooltip);
    textField.setAlignment(Pos.CENTER);

    final VBox box = FxLayout.newVBox(Pos.CENTER, Insets.EMPTY, textField, xLabel, label);
    box.setSpacing(-3);

    weightComponents.add(box);
    return component.valueProperty();
  }

  public void setValue(@Nullable CombinedScoreWeights value) {
    if (value == null) {
      ms2.setValue(null);
      isotopes.setValue(null);
      mz.setValue(null);
      rt.setValue(null);
      ri.setValue(null);
      ccs.setValue(null);
      return;
    }

    ms2.setValue(value.ms2());
    isotopes.setValue(value.isotopes());
    mz.setValue(value.mz());
    rt.setValue(value.rt());
    ri.setValue(value.ri());
    ccs.setValue(value.ccs());
  }

  public CombinedScoreWeights getValue() {
    return new CombinedScoreWeights(mz.getValue(), rt.getValue(), ri.getValue(), ccs.getValue(),
        ms2.getValue(), isotopes.getValue());
  }
}
