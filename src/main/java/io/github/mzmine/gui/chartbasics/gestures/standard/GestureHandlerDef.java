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

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.Handler;
import io.github.mzmine.gui.chartbasics.gestures.interf.GestureHandlerFactory;

/**
 * Definition of {@link ChartGestureHandler}s Used to store the definition and to generate
 * GestureHandlers
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GestureHandlerDef implements GestureHandlerFactory {

  protected Handler handler;
  protected Entity entity;
  protected Event[] event;
  protected GestureButton button;
  protected Key key;
  protected Object[] param;

  public GestureHandlerDef(Handler handler, Entity entity, Event[] event, GestureButton button, Key key,
      Object[] param) {
    super();
    this.handler = handler;
    this.entity = entity;
    this.event = event;
    this.button = button;
    this.key = key;
    this.param = param;
  }

  @Override
  public ChartGestureHandler createHandler() {
    return ChartGestureHandler.createHandler(handler, new ChartGesture(entity, event, button, key),
        param);
  }

  public Handler getHandler() {
    return handler;
  }

  public Entity getEntity() {
    return entity;
  }

  public Event[] getEvent() {
    return event;
  }

  public GestureButton getButton() {
    return button;
  }

  public Key getKey() {
    return key;
  }

  public Object[] getParam() {
    return param;
  }

  public void setHandler(Handler handler) {
    this.handler = handler;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public void setEvent(Event[] event) {
    this.event = event;
  }

  public void setButton(GestureButton button) {
    this.button = button;
  }

  public void setKey(Key key) {
    this.key = key;
  }

  public void setParam(Object[] param) {
    this.param = param;
  }

}
