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

package io.github.mzmine.gui.framework.strokechooser;

import java.awt.BasicStroke;
import java.awt.Graphics2D;

public class SettingsBasicStroke {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private float w, miterlimit, dashphase;
  private int cap, join;
  private float[] array;

  private transient BasicStroke stroke;

  public SettingsBasicStroke() {
    resetAll();
  }

  public SettingsBasicStroke(float w, int cap, int join, float miterlimit, float[] array,
      float dashphase) {
    this();
    setAll(w, cap, join, miterlimit, array, dashphase);

  }

  public SettingsBasicStroke(float w, int cap, int join, float miter) {
    this();
    setAll(w, cap, join, miter, new float[] {10.f}, 0);
  }

  public SettingsBasicStroke(float w) {
    this();
    setAll(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2, new float[] {10.f}, 0);
  }

  public SettingsBasicStroke(BasicStroke s) {
    this();
    setStroke(s);
  }

  public void setAll(float w, int cap, int join, float miterlimit, float[] array, float dashphase) {
    setLineWidth(w);
    setMiterlimit(miterlimit);
    setDashArray(array);
    setDashphase(dashphase);
    setCap(cap);
    setJoin(join);
  }

  public void setLineWidth(float w) {
    if (Math.abs(this.w - w) > 0.001) {
      this.w = w;
      stroke = null;
    }
  }

  public void setMiterlimit(float miterlimit) {
    if (Math.abs(this.miterlimit - miterlimit) > 0.00001) {
      this.miterlimit = miterlimit;
      stroke = null;
    }
  }

  public void setDashphase(float dashphase) {
    if (Math.abs(this.dashphase - dashphase) > 0.00001) {
      this.dashphase = dashphase;
      stroke = null;
    }
  }

  public void setCap(int cap) {
    if (this.cap != cap) {
      this.cap = cap;
      stroke = null;
    }
  }

  public void setJoin(int join) {
    if (this.join != join) {
      this.join = join;
      stroke = null;
    }
  }

  public void setDashArray(float[] array) {
    if (!this.array.equals(array)) {
      this.array = array;
      stroke = null;
    }
  }

  public void resetAll() {
    this.w = 1.5f;
    this.miterlimit = 2.f;
    this.dashphase = 0;
    this.cap = BasicStroke.CAP_ROUND;
    this.join = BasicStroke.JOIN_MITER;
    this.array = new float[] {10.f};
    stroke = null;
  }

  public BasicStroke getStroke() {
    if (stroke == null) {
      stroke = new BasicStroke(w, cap, join, miterlimit, array, dashphase);
    }
    return stroke;
  }

  public float[] getDashArray() {
    return array;
  }

  public float getDashPhase() {
    // TODO Auto-generated method stub
    return dashphase;
  }

  /**
   * Returns the line width. Line width is represented in user space, which is the
   * default-coordinate space used by Java 2D. See the <code>Graphics2D</code> class comments for
   * more information on the user space coordinate system.
   * 
   * @return the line width of this <code>BasicStroke</code>.
   * @see Graphics2D
   */
  public float getLineWidth() {
    return w;
  }

  /**
   * Returns the end cap style.
   * 
   * @return the end cap style of this <code>BasicStroke</code> as one of the static
   *         <code>int</code> values that define possible end cap styles.
   */
  public int getEndCap() {
    return cap;
  }

  /**
   * Returns the line join style.
   * 
   * @return the line join style of the <code>BasicStroke</code> as one of the static
   *         <code>int</code> values that define possible line join styles.
   */
  public int getLineJoin() {
    return join;
  }

  /**
   * Returns the limit of miter joins.
   * 
   * @return the limit of miter joins of the <code>BasicStroke</code>.
   */
  public float getMiterLimit() {
    return miterlimit;
  }

  public void setStroke(BasicStroke s) {
    setAll(s.getLineWidth(), s.getEndCap(), s.getLineJoin(), s.getMiterLimit(), s.getDashArray(),
        s.getDashPhase());
    stroke = s;
  }

}
