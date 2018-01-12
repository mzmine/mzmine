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

package net.sf.mzmine.framework.strokechooser;

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
