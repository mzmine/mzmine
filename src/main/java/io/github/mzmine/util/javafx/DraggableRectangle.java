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

package io.github.mzmine.util.javafx;

import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Rectangle;

public class DraggableRectangle extends Rectangle {

  public DraggableRectangle(double w, double h) {
    super(w, h);

    setOnDragDetected(e -> {
      Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
      ClipboardContent content = new ClipboardContent();
      int index = getParent().getChildrenUnmodifiable().indexOf(this);
      content.putString(String.valueOf(index));
      WritableImage snapshot = this.snapshot(null, null);
      dragboard.setDragView(snapshot);
      dragboard.setContent(content);
      e.consume();
    });

    setOnDragOver(e -> {
      if (e.getGestureSource() != this && e.getDragboard().hasString()) {
        e.acceptTransferModes(TransferMode.MOVE);
      }
      e.consume();
    });

    setOnDragEntered(e -> {
      if (e.getGestureSource() != this && e.getDragboard().hasString()) {
        setOpacity(getOpacity() * 0.3d);
      }
      e.consume();
    });

    setOnDragExited(e -> {
      if (e.getGestureSource() != this && e.getDragboard().hasString()) {
        setOpacity(getOpacity() / 0.3d);
      }
      e.consume();
    });

    setOnDragDropped(e -> {
      Parent parent = getParent();
      if (parent instanceof DraggableRectangleContainer) {

        Dragboard dragboard = e.getDragboard();
        if (!dragboard.hasString()) {
          return;
        }

        int oldIndex = Integer.valueOf(dragboard.getString());
        int newIndex = parent.getChildrenUnmodifiable().indexOf(this);

        ((DraggableRectangleContainer) parent)
            .moveRectangle(oldIndex, newIndex);
      }
      e.consume();
    });
  }
}
