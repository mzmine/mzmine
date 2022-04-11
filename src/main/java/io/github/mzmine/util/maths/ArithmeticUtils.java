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

import java.util.function.BinaryOperator;

public class ArithmeticUtils {

  /**
   * Addition operation for unknown Number subclass operands.
   *
   * @param <N> Subclass of the Number
   * @param o1 First operand
   * @param o2 Second operand
   * @return Result of the addition
   */
  public static <N extends Number> N add(N o1, N o2) {
    return binaryOperation(o1, o2, Double::sum, Float::sum, Integer::sum);
  }

  /**
   * Subtraction operation for unknown Number subclass operands.
   *
   * @param <N> Subclass of the Number
   * @param o1 First operand
   * @param o2 Second operand
   * @return Result of the subtraction
   */
  public static <N extends Number> N subtract(N o1, N o2) {
    return binaryOperation(o1, o2, (x1, x2) -> x1 - x2, (x1, x2) -> x1 - x2, (x1, x2) -> x1 - x2);
  }

  /**
   * Multiplication operation for unknown Number subclass operands.
   *
   * @param <N> Subclass of the Number
   * @param o1 First operand
   * @param o2 Second operand
   * @return Result of the multiplication
   */
  public static <N extends Number> N multiply(N o1, N o2) {
    return binaryOperation(o1, o2, (x1, x2) -> x1 * x2, (x1, x2) -> x1 * x2, (x1, x2) -> x1 * x2);
  }

  /**
   * Division operation for unknown Number subclass operands.
   *
   * @param <N> Subclass of the Number
   * @param o1 First operand
   * @param o2 Second operand
   * @return Result of the division
   */
  public static <N extends Number> N divide(N o1, N o2) {
    return binaryOperation(o1, o2, (x1, x2) -> x1 / x2, (x1, x2) -> x1 / x2, (x1, x2) -> x1 / x2);
  }

  /**
   * N x N -> N operation with explicitly defined behaviour for possible N types. Current
   * implementation supports Double, Float and Integer, but it can be easily extended to other
   * subclasses of the Number.
   *
   * @param <N> Subclass of the Number
   * @param o1 First operand
   * @param o2 Second operand
   * @param doubleOperator Operator defining Double behaviour
   * @param floatOperator Operator defining Float behaviour
   * @param intOperator Operator defining Integer behaviour
   * @return Result of the operation
   */
  private static <N extends Number> N binaryOperation(N o1, N o2,
      BinaryOperator<Double> doubleOperator,
      BinaryOperator<Float> floatOperator,
      BinaryOperator<Integer> intOperator) {
    if (o1 instanceof Double) {
      return (N) doubleOperator.apply(o1.doubleValue(), o2.doubleValue());
    } else if (o1 instanceof Float) {
      return (N) floatOperator.apply(o1.floatValue(), o2.floatValue());
    } else if (o1 instanceof Integer) {
      return (N) intOperator.apply(o1.intValue(), o2.intValue());
    } else {
      throw new IllegalArgumentException("Illegal operands types.");
    }
  }
}
