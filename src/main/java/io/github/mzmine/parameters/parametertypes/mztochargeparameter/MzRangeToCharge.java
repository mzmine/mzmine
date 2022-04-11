/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
