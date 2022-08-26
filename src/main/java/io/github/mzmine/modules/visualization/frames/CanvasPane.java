/* *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.frames;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

/**
 * Wraps a {@link Canvas} so it can be resized automatically.
 * <a href="https://stackoverflow.com/a/31761362">source</a>
 */
public class CanvasPane extends Pane {

  private final Canvas canvas;

  public CanvasPane(Canvas canvas) {
    this.canvas = canvas;
    getChildren().add(canvas);
  }

  public CanvasPane(double width, double height) {
    canvas = new Canvas(width, height);
    getChildren().add(canvas);
  }

  public Canvas getCanvas() {
    return canvas;
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    final double x = snappedLeftInset();
    final double y = snappedTopInset();
    final double w = snapSizeX(getWidth()) - x - snappedRightInset();
    final double h = snapSizeY(getHeight()) - y - snappedBottomInset();
    canvas.setLayoutX(x);
    canvas.setLayoutY(y);
    canvas.setWidth(w);
    canvas.setHeight(h);
  }
}
