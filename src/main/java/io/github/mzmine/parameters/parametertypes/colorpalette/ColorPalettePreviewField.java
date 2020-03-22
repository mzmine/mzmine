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
 */

package io.github.mzmine.parameters.parametertypes.colorpalette;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import io.github.mzmine.util.color.SimpleColorPalette;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A pane showing a color palette and allowing the selection of single colors within the palette.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class ColorPalettePreviewField extends FlowPane {

  private static final Logger logger = Logger.getLogger(ColorPalettePreviewField.class.getName());

  private static final int RECT_HEIGHT = 18;
  private static final Color STROKE_CLR = Color.WHITE;
  private static final double STROKE_WIDTH = 0.5;

  protected final List<Rectangle> rects;
  protected SimpleColorPalette palette;
  protected int selected;

  protected List<SelectionChangeListener> listeners;

  protected boolean validDrag;

  public ColorPalettePreviewField(SimpleColorPalette palette) {
    super();
    rects = new ArrayList<Rectangle>();
    setPalette(palette);

    setMinWidth(RECT_HEIGHT * 10);
    setMaxWidth(RECT_HEIGHT * 20);
    setPrefWidth(palette.size() * RECT_HEIGHT);

    validDrag = false;

    listeners = new ArrayList<>();
    palette.addListener((ListChangeListener<Color>) c -> {
      while (c.next()) {
        this.setPrefWidth(palette.size() * RECT_HEIGHT);
        if (c.wasRemoved() && selected >= palette.size()) {
          selected = palette.size() - 1;
        }
      }
      updatePreview();
    });
  }

  private void setRectangles() {
    rects.clear();

    if (palette == null || palette.isEmpty()) {
      return;
    }

    for (int i = 0; i < palette.size(); i++) {
      Color clr = palette.get(i);
      Rectangle rect = new Rectangle(RECT_HEIGHT - STROKE_WIDTH / 2,
          RECT_HEIGHT - STROKE_WIDTH / 2);
      rect.setFill(clr);
      rect.setStroke(Color.BLACK);
      rect.setStrokeWidth(STROKE_WIDTH);

      rect.setOnMousePressed(e -> {
        rect.setOpacity(rect.getOpacity() / 2);
        setSelected(rect);
      });

      rect.setOnMouseDragged(e -> {
        validDrag = true;
      });

      rect.setOnMouseReleased(e -> {
        rect.setOpacity(rect.getOpacity() * 2);

        if (!validDrag) {
          return;
        }

        Point2D exit = new Point2D(e.getSceneX(), e.getSceneY());

        double x = this.sceneToLocal(exit).getX();
        double y = this.sceneToLocal(exit).getY();
        int rows = (int) ((RECT_HEIGHT * palette.size()) / getWidth() + 1);

        x = (x < 0) ? 0 : x;
        y = (y < 0) ? 0 : y;
        y = (y / RECT_HEIGHT <= rows) ? y : rows;

        int rectsPerRow = (int) (getWidth() / RECT_HEIGHT);
        int row = (int) (y / RECT_HEIGHT);

        int newIndex = (int) (row * rectsPerRow + x / RECT_HEIGHT + .5);

        // we just have to move the color, the listener will update the preview
        palette.moveColor(getSelected(), newIndex);
        setSelected(newIndex);
        validDrag = false;
      });

      rects.add(rect);
    }

    if (selected < rects.size() && selected >= 0) {
      rects.get(selected).setStroke(STROKE_CLR);
      rects.get(selected).setStrokeWidth(STROKE_WIDTH);
    }
  }

  private void setSelected(Rectangle rect) {
    setSelected(rects.indexOf(rect));
  }

  private void setSelected(int i) {
    if (i < 0 || i >= palette.size()) {
      return;
    }
    this.selected = i;
    updatePreview();
    listeners.forEach(l -> l.selectionChanged(palette.get(getSelected()), getSelected()));
  }

  public int getSelected() {
    return selected;
  }

  public void updatePreview() {
    setRectangles();
    getChildren().clear();
    getChildren().addAll(rects);
  }

  public SimpleColorPalette getPalette() {
    return palette;
  }

  public void setPalette(SimpleColorPalette palette) {
    this.palette = palette;
    updatePreview();
  }

  public boolean addListener(SelectionChangeListener listener) {
    return listeners.add(listener);
  }

  public boolean removeListener(SelectionChangeListener listener) {
    return listeners.remove(listener);
  }
}
