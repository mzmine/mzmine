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

package io.github.mzmine.util.maths;

/**
 * A list of math operators and their string representation
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum MathOperator {
  GREATER(">"), GREATER_EQ("≥"), EQUAL("="), NOT_EQUAL("≠"), LESS("<"), LESS_EQ("≤");

  private final String s;

  MathOperator(String s) {
    this.s = s;
  }

  @Override
  public String toString() {
    return s;
  }

  public boolean checkValue(double base, double testedVal) {
    return switch (this) {
      case LESS -> testedVal < base;
      case LESS_EQ -> testedVal <= base;
      case GREATER -> testedVal > base;
      case GREATER_EQ -> testedVal >= base;
      case EQUAL -> testedVal == base;
      case NOT_EQUAL -> testedVal != base;
    };
  }
}
