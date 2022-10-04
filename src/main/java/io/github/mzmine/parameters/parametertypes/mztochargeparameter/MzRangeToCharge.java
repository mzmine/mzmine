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

package io.github.mzmine.parameters.parametertypes.mztochargeparameter;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Represents a mz range and a corresponding charge. Used in {@link MzToChargeParameter} in case a
 * default charge shall be assumed for calculations. Used to represent these values in a java fx
 * {@link javafx.scene.control.TableView}.
 */
public class MzRangeToCharge {

  private DoubleProperty lower;
  private DoubleProperty upper;
  private IntegerProperty charge;

  public MzRangeToCharge(double lower, double upper, int charge) {
    this.lower = new SimpleDoubleProperty(lower);
    this.upper = new SimpleDoubleProperty(upper);
    this.charge = new SimpleIntegerProperty(charge);

    this.lower.addListener((observable, oldValue, newValue) -> {
      if (newValue.doubleValue() > this.upper.doubleValue()) {
        this.upper.set(newValue.doubleValue());
      }
    });

    this.upper.addListener((observable, oldValue, newValue) -> {
      if (newValue.doubleValue() < this.lower.doubleValue()) {
        this.lower.set(newValue.doubleValue());
      }
    });
  }

  public double getLower() {
    return lower.get();
  }

  public void setLower(double lower) {
    this.lower.set(lower);
  }

  public DoubleProperty lowerProperty() {
    return lower;
  }

  public double getUpper() {
    return upper.get();
  }

  public void setUpper(double upper) {
    this.upper.set(upper);
  }

  public DoubleProperty upperProperty() {
    return upper;
  }

  public int getCharge() {
    return charge.get();
  }

  public void setCharge(int charge) {
    this.charge.set(charge);
  }

  public IntegerProperty chargeProperty() {
    return charge;
  }
}
