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
