package net.sf.mzmine.chartbasics.javafx.mouse;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javafx.scene.input.ScrollEvent;

public class MouseEventWrapper {

  public enum Type {
    SWING, FX_MOUSE, FX_SCROLL;
  }

  private Type type;
  private MouseEvent swing;
  private javafx.scene.input.MouseEvent fx;
  private ScrollEvent scrollfx;

  public MouseEventWrapper(javafx.scene.input.MouseEvent fx) {
    super();
    this.fx = fx;
    type = Type.FX_MOUSE;
  }

  public MouseEventWrapper(MouseEvent swing) {
    super();
    this.swing = swing;
    type = Type.SWING;
  }

  public MouseEventWrapper(ScrollEvent scrollfx) {
    super();
    this.scrollfx = scrollfx;
    type = Type.FX_SCROLL;
  }

  public boolean isFXEvent() {
    return !isSwingEvent();
  }

  public boolean isFXMouseEvent() {
    return type.equals(Type.FX_MOUSE);
  }

  public boolean isFXScrollEvent() {
    return type.equals(Type.FX_SCROLL);
  }

  public boolean isSwingEvent() {
    return type.equals(Type.SWING);
  }

  public Object getEvent() {
    switch (type) {
      case SWING:
        return swing;
      case FX_MOUSE:
        return fx;
      case FX_SCROLL:
        return scrollfx;
    }
    return null;
  }

  public boolean isAltDown() {
    switch (type) {
      case SWING:
        return swing.isAltDown();
      case FX_MOUSE:
        return fx.isAltDown();
      case FX_SCROLL:
        return scrollfx.isAltDown();
    }
    return false;
  }

  public boolean isShiftDown() {
    switch (type) {
      case SWING:
        return swing.isShiftDown();
      case FX_MOUSE:
        return fx.isShiftDown();
      case FX_SCROLL:
        return scrollfx.isShiftDown();
    }
    return false;
  }

  public boolean isControlDown() {
    switch (type) {
      case SWING:
        return swing.isControlDown();
      case FX_MOUSE:
        return fx.isControlDown();
      case FX_SCROLL:
        return scrollfx.isControlDown();
    }
    return false;
  }

  public boolean isMetaDown() {
    switch (type) {
      case SWING:
        return swing.isMetaDown();
      case FX_MOUSE:
        return fx.isMetaDown();
      case FX_SCROLL:
        return scrollfx.isMetaDown();
    }
    return false;
  }

  public boolean isConsumed() {
    switch (type) {
      case SWING:
        return swing.isConsumed();
      case FX_MOUSE:
        return fx.isConsumed();
      case FX_SCROLL:
        return scrollfx.isConsumed();
    }
    return false;
  }

  public double getX() {
    switch (type) {
      case SWING:
        return swing.getX();
      case FX_MOUSE:
        return fx.getX();
      case FX_SCROLL:
        return scrollfx.getX();
    }
    return 0;
  }

  public double getY() {
    switch (type) {
      case SWING:
        return swing.getY();
      case FX_MOUSE:
        return fx.getY();
      case FX_SCROLL:
        return scrollfx.getY();
    }
    return 0;
  }

  public double getXOnScreen() {
    switch (type) {
      case SWING:
        return swing.getXOnScreen();
      case FX_MOUSE:
        return fx.getScreenX();
      case FX_SCROLL:
        return scrollfx.getScreenX();
    }
    return 0;
  }

  public double getYOnScreen() {
    switch (type) {
      case SWING:
        return swing.getYOnScreen();
      case FX_MOUSE:
        return fx.getScreenY();
      case FX_SCROLL:
        return scrollfx.getScreenY();
    }
    return 0;
  }

  public int getClickCount() {
    switch (type) {
      case SWING:
        return swing.getClickCount();
      case FX_MOUSE:
        return fx.getClickCount();
      case FX_SCROLL:
        return scrollfx.getTouchCount();
    }
    return 0;
  }

  /**
   * Returns which, if any, of the mouse buttons has changed state. The returned value is ranged
   * from 0 to the {@link java.awt.MouseInfo#getNumberOfButtons() MouseInfo.getNumberOfButtons()}
   * value. The returned value includes at least the following constants:
   * <ul>
   * <li>{@code NOBUTTON}
   * <li>{@code BUTTON1}
   * <li>{@code BUTTON2}
   * <li>{@code BUTTON3}
   * </ul>
   * It is allowed to use those constants to compare with the returned button number in the
   * application. For example,
   * 
   * <pre>
   * if (anEvent.getButton() == MouseEvent.BUTTON1) {
   * </pre>
   * 
   * In particular, for a mouse with one, two, or three buttons this method may return the following
   * values:
   * <ul>
   * <li>0 ({@code NOBUTTON})
   * <li>1 ({@code BUTTON1})
   * <li>2 ({@code BUTTON2})
   * <li>3 ({@code BUTTON3})
   * </ul>
   * Button numbers greater then {@code BUTTON3} have no constant identifier. So if a mouse with
   * five buttons is installed, this method may return the following values:
   * <ul>
   * <li>0 ({@code NOBUTTON})
   * <li>1 ({@code BUTTON1})
   * <li>2 ({@code BUTTON2})
   * <li>3 ({@code BUTTON3})
   * <li>4
   * <li>5
   * </ul>
   * <p>
   * Note: If support for extended mouse buttons is {@link Toolkit#areExtraMouseButtonsEnabled()
   * disabled} by Java then the AWT event subsystem does not produce mouse events for the extended
   * mouse buttons. So it is not expected that this method returns anything except {@code NOBUTTON},
   * {@code BUTTON1}, {@code BUTTON2}, {@code BUTTON3}.
   *
   * @return one of the values from 0 to {@link java.awt.MouseInfo#getNumberOfButtons()
   *         MouseInfo.getNumberOfButtons()} if support for the extended mouse buttons is
   *         {@link Toolkit#areExtraMouseButtonsEnabled() enabled} by Java. That range includes
   *         {@code NOBUTTON}, {@code BUTTON1}, {@code BUTTON2}, {@code BUTTON3}; <br>
   *         {@code NOBUTTON}, {@code BUTTON1}, {@code BUTTON2} or {@code BUTTON3} if support for
   *         the extended mouse buttons is {@link Toolkit#areExtraMouseButtonsEnabled() disabled} by
   *         Java
   * @since 1.4
   * @see Toolkit#areExtraMouseButtonsEnabled()
   * @see java.awt.MouseInfo#getNumberOfButtons()
   * @see #MouseEvent(Component, int, long, int, int, int, int, int, int, boolean, int)
   * @see InputEvent#getMaskForButton(int)
   */
  public int getButton() {
    switch (type) {
      case SWING:
        return swing.getButton();
      case FX_MOUSE:
        switch (fx.getButton()) {
          case NONE:
            return 0;
          case PRIMARY:
            return 1;
          case SECONDARY:
            return 2;
          case MIDDLE:
            return 3;
        }
      case FX_SCROLL:
        return 3;
    }
    return 0;
  }

  public Object getSource() {
    switch (type) {
      case SWING:
        return swing.getSource();
      case FX_MOUSE:
        return fx.getSource();
      case FX_SCROLL:
        return scrollfx.getSource();
    }
    return null;
  }

  public boolean isMouseWheelEvent() {
    switch (type) {
      case SWING:
        return swing instanceof MouseWheelEvent;
      case FX_MOUSE:
        return false;
      case FX_SCROLL:
        return true;
    }
    return false;
  }

  public double getWheelRotation() {
    switch (type) {
      case SWING:
        if (isMouseWheelEvent())
          return ((MouseWheelEvent) swing).getPreciseWheelRotation();
        else
          return 0;
      case FX_SCROLL:
        return scrollfx.getDeltaY();
    }
    return 0;
  }

  public void consume() {
    switch (type) {
      case SWING:
        swing.consume();
        break;
      case FX_MOUSE:
        fx.consume();
        break;
      case FX_SCROLL:
        scrollfx.consume();
        break;
    }

  }


}
