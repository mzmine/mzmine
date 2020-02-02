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
 *
 */
public class ColorPalettePreviewField extends FlowPane {

  private static final Logger logger = Logger.getLogger(ColorPalettePreviewField.class.getName());

  private static final int RECT_HEIGHT = 17;
  private static final Color OUTLINE_CLR = Color.BLACK;

  protected final List<Rectangle> rects;
  protected SimpleColorPalette palette;
  protected int selected;

  public ColorPalettePreviewField(SimpleColorPalette palette) {
    super();
    rects = new ArrayList<Rectangle>();
    setMaxWidth(400);
    setPalette(palette);
    
    palette.addListener(new ListChangeListener<Color> () {
      @Override
      public void onChanged(Change<? extends Color> c) {
        updatePreview();
      }
    });
  }

  private void setRectangles() {
    rects.clear();

    if (palette == null || palette.isEmpty())
      return;

    for (int i = 0; i < palette.size(); i++) {
      Color clr = palette.get(i);
      Rectangle rect = new Rectangle(RECT_HEIGHT, RECT_HEIGHT);
      rect.setFill(clr);
      rect.setOnMouseClicked(e -> {
        if (e.getClickCount() == 1) {
          setSelected(rect);
        }
      });

      rect.setOnMousePressed(e -> {
        rect.setOpacity(rect.getOpacity() / 2);
      });

      rect.setOnMouseReleased(e -> {
        rect.setOpacity(rect.getOpacity() * 2);
        Point2D exit = new Point2D(e.getSceneX(), e.getSceneY());
        
        double x = this.sceneToLocal(exit).getX();
        int newIndex = (int) (x / RECT_HEIGHT + .5);

        // we just have to move the color, the listener will update the preview
        palette.moveColor(rects.indexOf(rect), newIndex);
      });
      
      rects.add(rect);
    }

    if (selected < rects.size()) {
      rects.get(selected).setStroke(OUTLINE_CLR);
      rects.get(selected).setStrokeWidth(1.0);
    }
  }

  private void setSelected(Rectangle rect) {
    setSelected(rects.indexOf(rect));
  }

  private void setSelected(int i) {
    this.selected = i;
    updatePreview();
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
}
