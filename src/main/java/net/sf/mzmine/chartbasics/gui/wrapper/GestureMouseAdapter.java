/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.chartbasics.gui.wrapper;

import net.sf.mzmine.chartbasics.gestures.ChartGesture.Button;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Event;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Key;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler.DragHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler.Handler;

public interface GestureMouseAdapter {

  /**
   * Add drag handlers for each key (key and handler have to be ordered)
   * 
   * @param g
   * @param handler
   */
  public void addDragGestureHandler(DragHandler[] handler, Key[] key, Entity entity, Button button,
      Orientation orient, Object[] param);

  /**
   * Add a preset handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void addGestureHandler(Handler handler, Entity entity, Event[] event, Button button,
      Key key, Object[] param);

  /**
   * Add a handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void addGestureHandler(ChartGestureHandler handler);

  /**
   * Add a handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void removeGestureHandler(ChartGestureHandler handler);
}
