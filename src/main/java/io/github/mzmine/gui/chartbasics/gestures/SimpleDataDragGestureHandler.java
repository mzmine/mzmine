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

package io.github.mzmine.gui.chartbasics.gestures;

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import java.awt.geom.Point2D;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class SimpleDataDragGestureHandler extends ChartGestureHandler {

  private static final Logger logger = Logger.getLogger(
      SimpleDataDragGestureHandler.class.getName());

  protected Point2D start = null;
  protected Point2D end = null;
  protected final BiConsumer<Point2D, Point2D> onAction;


  /**
   * Creates a drag event handler for Mouse1 + Ctrl.
   *
   * @param onAction The action to be executed when a drag is finished. Provides the start and end
   *                 coordinates of the drag in plot data units as {@link Point2D}.
   */
  public SimpleDataDragGestureHandler(BiConsumer<Point2D, Point2D> onAction) {
    this(GestureButton.BUTTON1, Key.CTRL, onAction);
  }

  /**
   * @param mouseButton    The mouse button for the event.
   * @param keyboardButton The keyboard button for the event.
   * @param onAction       The action to be executed when a drag is finished. Provides the start and
   *                       end coordinates of the drag in plot data units as {@link Point2D}.
   */
  public SimpleDataDragGestureHandler(GestureButton mouseButton, Key keyboardButton,
      BiConsumer<Point2D, Point2D> onAction) {
    super(new ChartGesture(Entity.ALL_PLOT_AND_DATA, new Event[]{Event.PRESSED, Event.RELEASED},
        mouseButton, keyboardButton));
    this.onAction = onAction;

    setConsumer(chartGestureEvent -> {
      if (chartGestureEvent.getMouseEvent().isPressed() && chartGestureEvent.getMouseEvent()
          .isControlDown()) {
        start = chartGestureEvent.getCoordinates();
        chartGestureEvent.getMouseEvent().consume();
        logger.finest("Drag event started at " + start.toString());
      }

      if (chartGestureEvent.getMouseEvent().isReleased()) {
        end = chartGestureEvent.getCoordinates();
        if (start != null) {
          logger.finest("Drag event ended at " + end.toString());
          onAction.accept(start, end);
        }
        start = null;
        end = null;
      }
    });
  }

}
