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

package io.github.mzmine.parameters.parametertypes.paintscale;

import io.github.mzmine.parameters.parametertypes.colorpalette.SelectionChangeListener;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.DraggableRectangle;
import io.github.mzmine.util.javafx.DraggableRectangleContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A pane showing a color palette and allowing the selection of single colors within the palette.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class PaintScalePalettePreviewField extends FlowPane implements DraggableRectangleContainer {

  private static final Logger logger = Logger
      .getLogger(PaintScalePalettePreviewField.class.getName());

  private static final int RECT_HEIGHT = 18;
  private static final Color STROKE_CLR_SELECTED = Color.WHITE;
  private static final Color STROKE_CLR_DEFAULT = Color.BLACK;
  private static final double STROKE_WIDTH = 0.5;

  protected final List<Rectangle> rects;
  protected SimpleColorPalette palette;
  protected int selected;

  protected List<SelectionChangeListener> listeners;

  protected boolean validDrag;

  public PaintScalePalettePreviewField(SimpleColorPalette palette) {
    super();
    rects = new ArrayList<>();
    listeners = new ArrayList<>();
    setPalette(palette);

    setMinWidth(RECT_HEIGHT * 10);
    setMaxWidth(RECT_HEIGHT * 20);
    setPrefWidth(palette.size() * RECT_HEIGHT);

    validDrag = false;

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
      Rectangle rect = new DraggableRectangle(RECT_HEIGHT - STROKE_WIDTH / 2,
          RECT_HEIGHT - STROKE_WIDTH / 2);

      rects.add(rect);

      rect.setFill(clr);
      rect.setStroke(STROKE_CLR_DEFAULT);
      rect.setStrokeWidth(STROKE_WIDTH);

      rect.setOnMousePressed(e -> {
        setSelected(rect);
      });
    }
  }

  public int getSelected() {
    return selected;
  }

  private void setSelected(Rectangle rect) {
    setSelected(rects.indexOf(rect));
  }

  private void setSelected(int i) {
    if (i < 0 || i >= palette.size()) {
      return;
    }
    this.selected = i;
    rects.forEach(r -> r.setStroke(STROKE_CLR_DEFAULT));
    rects.get(i).setStroke(STROKE_CLR_SELECTED);
    listeners.forEach(l -> l.selectionChanged(palette.get(getSelected()), getSelected()));
  }

  public void updatePreview() {
    setRectangles();
    getChildren().clear();
    getChildren().addAll(rects);
    setSelected(selected);
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

  /**
   * When a rectangle is drag and dropped this method is called by the rectangles, the actual moving
   * is done here.
   *
   * @param oldIndex
   * @param newIndex
   */
  @Override
  public void moveRectangle(int oldIndex, int newIndex) {
    newIndex = palette.moveColor(oldIndex, newIndex);
    if (newIndex != -1) {
      updatePreview();
      setSelected(newIndex);
    }
  }
}
