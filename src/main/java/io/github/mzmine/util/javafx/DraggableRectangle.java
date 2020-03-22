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

import java.util.logging.Logger;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Rectangle;

public class DraggableRectangle extends Rectangle {

  private static final Logger logger = Logger.getLogger("test");

  public DraggableRectangle(double w, double h) {
    super(w, h);

    setOnDragDetected(e -> {
      logger.info("drag detected");
//      if(this.getScene() == null)
//        return;

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
      logger.info("drag over");
      if (e.getGestureSource() != this && e.getGestureSource() instanceof Rectangle) {
        e.acceptTransferModes(TransferMode.MOVE);
      }
      e.consume();
    });

    setOnDragEntered(e -> {
      logger.info("drag entered");
      if (e.getGestureSource() != this && e.getDragboard().hasString()) {
        setOpacity(getOpacity() * 0.3d);
      }
      e.consume();
    });

    setOnDragExited(e -> {
      logger.info("drag exited");
      if (e.getGestureSource() != this && e.getDragboard().hasString()) {
        setOpacity(getOpacity() / 0.3d);
      }
    });

    setOnDragDropped(e -> {
      logger.info("drag dropped");

      Parent parent = getParent();
      if (parent instanceof DraggableRectangleContainer) {

        Dragboard dragboard = e.getDragboard();
        if (!dragboard.hasString()) {
          return;
        }

        int oldIndex = Integer.valueOf(dragboard.getString());
        int newIndex = parent.getChildrenUnmodifiable().indexOf(this);

        logger.info(
            "drag dropped - old: " + oldIndex + " new: "
                + newIndex);

        ((DraggableRectangleContainer) parent)
            .moveRectangle(parent.getChildrenUnmodifiable().indexOf(this), newIndex);
      }
    });
  }
}
