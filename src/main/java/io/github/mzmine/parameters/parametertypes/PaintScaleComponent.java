/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 *
 */

package io.github.mzmine.parameters.parametertypes;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PaintScaleComponent extends GridPane {

  private final SwingNode swingNode;
  private final ComboBox<PaintScale> comboBox;

  public PaintScaleComponent(ObservableList<PaintScale> choices) {
    comboBox = new ComboBox<>(choices);
    comboBox.setMinWidth(100);
    comboBox.valueProperty().addListener(new ChangeListener<PaintScale>() {
      @Override
      public void changed(ObservableValue<? extends PaintScale> observable, PaintScale oldValue,
          PaintScale newValue) {
        drawLegendInSwingNode();
      }
    });
    add(comboBox, 0, 0);
    swingNode = new SwingNode();
    swingNode.prefWidth(100);
    swingNode.prefHeight(10);
    drawLegendInSwingNode();
    add(swingNode, 0, 1);
  }


  private void drawLegendInSwingNode() {
    JPanel jPanel = new JPanel() {
      @Override
      public void paintComponent(Graphics g) {
        super.paintComponent(g);
        PaintScale selectedPaintScale = comboBox.getSelectionModel().getSelectedItem();
        PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
        paintScaleFactoy.createColorsForPaintScale(selectedPaintScale);
        for (int i = 0; i < 100; i++) {
          g.setColor((Color) selectedPaintScale.getPaint(i));
          g.drawRect(0 + i, 1, 1, 10);
        }
      }
    };
    jPanel.setSize(100, 10);
    swingNode.setContent(jPanel);
  }


  public SwingNode getSwingNode() {
    return swingNode;
  }


  public ComboBox<PaintScale> getComboBox() {
    return comboBox;
  }

}
