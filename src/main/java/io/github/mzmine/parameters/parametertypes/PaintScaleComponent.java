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
