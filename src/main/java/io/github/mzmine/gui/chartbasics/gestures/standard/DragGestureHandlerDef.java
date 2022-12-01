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

package io.github.mzmine.gui.chartbasics.gestures.standard;

import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.DragHandler;
import io.github.mzmine.gui.chartbasics.gestures.interf.GestureHandlerFactory;

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
  protected GestureButton button;
  protected Orientation orient;
  protected Object[] param;

  public DragGestureHandlerDef(DragHandler[] handler, Key[] key, Entity entity, GestureButton button,
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

  public GestureButton getButton() {
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

  public void setButton(GestureButton button) {
    this.button = button;
  }

  public void setOrient(Orientation orient) {
    this.orient = orient;
  }

  public void setParam(Object[] param) {
    this.param = param;
  }

}
