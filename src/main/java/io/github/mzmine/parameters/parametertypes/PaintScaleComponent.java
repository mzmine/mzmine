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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PaintScaleComponent extends GridPane {

  private final ComboBox<PaintScale> comboBox;
  private final FlowPane flowPane;

  public PaintScaleComponent(ObservableList<PaintScale> choices) {
    comboBox = new ComboBox<>(choices);
    comboBox.setMinWidth(100);
    comboBox.valueProperty().addListener(new ChangeListener<PaintScale>() {
      @Override
      public void changed(ObservableValue<? extends PaintScale> observable, PaintScale oldValue,
          PaintScale newValue) {
        drawPaintScale(newValue);
      }
    });
    add(comboBox, 0, 0);
    flowPane = new FlowPane();
    add(flowPane, 0, 1);
  }


  private void drawPaintScale(PaintScale paintScale) {
    PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
    paintScale = paintScaleFactoy.createColorsForPaintScale(paintScale);
    List<Stop> stops = new ArrayList<>(100);
    for (int i = 0; i < 100; i++) {
      stops.add(
          new Stop((double) i / 100, FxColorUtil.awtColorToFX((Color) paintScale.getPaint(i))));
    }
    LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
    flowPane.getChildren().clear();
    flowPane.getChildren().add(new Rectangle(100, 10, gradient));
  }

  public ComboBox<PaintScale> getComboBox() {
    return comboBox;
  }

}
