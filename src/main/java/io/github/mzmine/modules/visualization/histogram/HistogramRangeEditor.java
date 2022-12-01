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

package io.github.mzmine.modules.visualization.histogram;

import java.text.NumberFormat;
import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;

public class HistogramRangeEditor extends BorderPane {

  private ComboBox<HistogramDataType> dataTypeCombo;
  private DoubleRangeComponent dataRangeComponent;

  public HistogramRangeEditor() {

    dataTypeCombo = new ComboBox<HistogramDataType>(
        FXCollections.observableArrayList(HistogramDataType.values()));
    dataTypeCombo.getSelectionModel().select(0);
    dataTypeCombo.setOnAction(e -> {
      HistogramDataType selectedType = dataTypeCombo.getSelectionModel().getSelectedItem();
      if (selectedType == null)
        return;

      switch (selectedType) {
        case MASS:
          dataRangeComponent.setNumberFormat(MZmineCore.getConfiguration().getMZFormat());
          return;
        case HEIGHT:
        case AREA:
          dataRangeComponent.setNumberFormat(MZmineCore.getConfiguration().getIntensityFormat());
          return;
        case RT:
          dataRangeComponent.setNumberFormat(MZmineCore.getConfiguration().getRTFormat());
          return;
      }
    });
    setLeft(dataTypeCombo);

    dataRangeComponent = new DoubleRangeComponent(NumberFormat.getNumberInstance());
    setCenter(dataRangeComponent);

  }

  public void setValue(Range<Double> value) {
    dataRangeComponent.setValue(value);
  }

  public HistogramDataType getSelectedType() {
    return dataTypeCombo.getSelectionModel().getSelectedItem();
  }

  public Range<Double> getValue() {
    return dataRangeComponent.getValue();
  }



}
