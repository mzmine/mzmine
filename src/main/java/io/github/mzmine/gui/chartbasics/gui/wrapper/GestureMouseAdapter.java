/*
 * Copyright 2006-2020 The MZmine Development Team
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
package io.github.mzmine.gui.chartbasics.gui.wrapper;

import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Button;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.DragHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.Handler;

public interface GestureMouseAdapter {

    /**
     * Add drag handlers for each key (key and handler have to be ordered)
     * 
     * @param g
     * @param handler
     */
    public void addDragGestureHandler(DragHandler[] handler, Key[] key,
            Entity entity, Button button, Orientation orient, Object[] param);

    /**
     * Add a preset handler for specific gestures and ChartMouseGestureEvents
     * 
     * @param g
     * @param handler
     */
    public void addGestureHandler(Handler handler, Entity entity, Event[] event,
            Button button, Key key, Object[] param);

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
