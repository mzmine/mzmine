/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.chartbasics.gestures.standard;

import net.sf.mzmine.chartbasics.gestures.ChartGesture.Button;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Key;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler.DragHandler;
import net.sf.mzmine.chartbasics.gestures.interf.GestureHandlerFactory;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
/**
 * Definition of {@link ChartGestureDragDiffHandler}s Used to store the definition of a drag
 * difference handler and to generate GestureHandlers
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class DragGestureHandlerDef implements GestureHandlerFactory {

  protected DragHandler[] handler;
  protected Key[] key;
  protected Entity entity;
  protected Button button;
  protected Orientation orient;
  protected Object[] param;

  public DragGestureHandlerDef(DragHandler[] handler, Key[] key, Entity entity, Button button,
      Orientation orient, Object[] param) {
    super();
    this.handler = handler;
    this.key = key;
    this.entity = entity;
    this.button = button;
    this.orient = orient;
    this.param = param;
  }

  @Override
  public ChartGestureHandler createHandler() {
    return ChartGestureHandler.createDragDiffHandler(handler, key, entity, button, orient, param);
  }

  public DragHandler[] getHandler() {
    return handler;
  }

  public Key[] getKey() {
    return key;
  }

  public Entity getEntity() {
    return entity;
  }

  public Button getButton() {
    return button;
  }

  public Orientation getOrient() {
    return orient;
  }

  public Object[] getParam() {
    return param;
  }

  public void setHandler(DragHandler[] handler) {
    this.handler = handler;
  }

  public void setKey(Key[] key) {
    this.key = key;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public void setButton(Button button) {
    this.button = button;
  }

  public void setOrient(Orientation orient) {
    this.orient = orient;
  }

  public void setParam(Object[] param) {
    this.param = param;
  }



}
