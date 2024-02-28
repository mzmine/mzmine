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
