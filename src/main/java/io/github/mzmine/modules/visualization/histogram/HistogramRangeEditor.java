/*
 * Copyright 2006-2021 The MZmine Development Team
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
