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
package io.github.mzmine.modules.dataprocessing.align_adap3.algorithms;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class contains mathematical functions
 *
 * @author aleksandrsmirnov Modified by Dharak Shah to include in MSDK
 */
public class Math {

  /**
   * <p>
   * interpolate.
   * </p>
   *
   * @param x_values a {@link SortedSet} object.
   * @param y_values a {@link NavigableMap} object.
   * @return an array of double.
   */
  static public double[] interpolate(final SortedSet<Double> x_values,
      final NavigableMap<Double, Double> y_values) {
    double[] result = new double[x_values.size()];

    int index = 0;
    final Entry<Double, Double> firstEntry = y_values.firstEntry();
    Iterator<Entry<Double, Double>> it = y_values.entrySet().iterator();
    Entry<Double, Double> entry = it.next();

    for (double x : x_values) {
      // Loop over y_values until we find x
      while (entry.getKey() < x && it.hasNext())
        entry = it.next();

      // Get y-value and interpolate if necessary
      double key = entry.getKey();
      if (key == x) // y_values contains x
        result[index++] = entry.getValue();
      else if (entry.equals(firstEntry)) // x is less the any in y_values
        index++;
      else if (x < key) { // Interpolate
        Entry<Double, Double> prevEntry = y_values.lowerEntry(key);
        result[index++] =
            (entry.getValue() - prevEntry.getValue()) / (entry.getKey() - prevEntry.getKey())
                * (x - prevEntry.getKey()) + prevEntry.getValue();
      } else
        index++;
    }

    return result;
  }

  /**
   * <p>
   * interpolate.
   * </p>
   *
   * @param x_value a double.
   * @param y_values a {@link NavigableMap} object.
   * @return a double.
   */
  static public double interpolate(final double x_value,
      final NavigableMap<Double, Double> y_values) {
    SortedSet<Double> x_values = new TreeSet<>();
    x_values.add(x_value);

    double[] interpolatedValues = interpolate(x_values, y_values);

    if (interpolatedValues.length != 1)
      throw new IllegalArgumentException("Cannot interpolate " + y_values + " at value " + x_value);

    return interpolatedValues[0];
  }

  /**
   * <p>
   * continuous_dot_product.
   * </p>
   *
   * @param f1 a {@link NavigableMap} object.
   * @param f2 a {@link NavigableMap} object.
   * @return a double.
   */
  static public double continuous_dot_product(final NavigableMap<Double, Double> f1,
      final NavigableMap<Double, Double> f2) {
    // Create a sorted union of x-values
    SortedSet<Double> x_union = new TreeSet<>(f1.navigableKeySet());
    SortedSet<Double> x_union2 = new TreeSet<>(f2.navigableKeySet());
    x_union.addAll(x_union2);

    double[] f1interpolated = interpolate(x_union, f1);
    double[] f2interpolated = interpolate(x_union, f2);

    NavigableMap<Double, Double> product = new TreeMap<>();
    int index = 0;
    for (double x : x_union) {
      product.put(x, f1interpolated[index] * f2interpolated[index]);
      ++index;
    }

    return integrate(product);
  }

  /**
   * <p>
   * continuous_dot_product.
   * </p>
   *
   * @param f1 a {@link Map} object.
   * @param f2 a {@link Map} object.
   * @return a double.
   */
  static public double continuous_dot_product(final Map<Double, Double> f1,
      final Map<Double, Double> f2) {
    return continuous_dot_product(new TreeMap<Double, Double>(f1), new TreeMap<Double, Double>(f2));
  }

  /**
   * <p>
   * pearson_correlation.
   * </p>
   *
   * @param f1 a {@link NavigableMap} object.
   * @param f2 a {@link NavigableMap} object.
   * @return a double.
   */
  static public double pearson_correlation(final NavigableMap<Double, Double> f1,
      final NavigableMap<Double, Double> f2) {
    // Create a sorted union of x-values
    SortedSet<Double> x_union = new TreeSet<>(f1.navigableKeySet());
    SortedSet<Double> x_union2 = new TreeSet<>(f2.navigableKeySet());
    x_union.addAll(x_union2);

    double[] f1interpolated = interpolate(x_union, f1);
    double[] f2interpolated = interpolate(x_union, f2);

    int size = x_union.size();

    double sum_x = 0.0;
    double sum_y = 0.0;
    double sum_xy = 0.0;
    double sum_x2 = 0.0;
    double sum_y2 = 0.0;

    for (int i = 0; i < size; ++i) {
      double x = f1interpolated[i];
      double y = f2interpolated[i];

      sum_x += x;
      sum_y += y;
      sum_xy += x * y;
      sum_x2 += x * x;
      sum_y2 += y * y;
    }

    double ave_x = sum_x / size;
    double ave_y = sum_y / size;
    double ave_xy = sum_xy / size;
    double ave_x2 = sum_x2 / size;
    double ave_y2 = sum_y2 / size;

    return (ave_xy - ave_x * ave_y) / java.lang.Math.sqrt(ave_x2 - ave_x * ave_x)
        / java.lang.Math.sqrt(ave_y2 - ave_y * ave_y);
  }

  /**
   * <p>
   * integrate.
   * </p>
   *
   * @param func a {@link NavigableMap} object.
   * @return a double.
   */
  static public double integrate(final NavigableMap<Double, Double> func) {
    Iterator<Entry<Double, Double>> it = func.entrySet().iterator();
    Entry<Double, Double> prevEntry = it.next();

    double result = 0.0;

    while (it.hasNext()) {
      Entry<Double, Double> entry = it.next();

      result +=
          0.5 * (prevEntry.getValue() + entry.getValue()) * (entry.getKey() - prevEntry.getKey());

      prevEntry = entry;
    }

    return result;
  }

  /**
   * <p>
   * discrete_dot_product.
   * </p>
   *
   * @param f1 a {@link NavigableMap} object.
   * @param f2 a {@link NavigableMap} object.
   * @param tolerance a double.
   * @return a double.
   */
  static public double discrete_dot_product(final NavigableMap<Double, Double> f1,
      final NavigableMap<Double, Double> f2, double tolerance) {
    double result = 0.0;

    if (f1.isEmpty() || f2.isEmpty())
      return result;

    Iterator<Entry<Double, Double>> it1 = f1.entrySet().iterator();
    Iterator<Entry<Double, Double>> it2 = f2.entrySet().iterator();

    Entry<Double, Double> entry1 = it1.next();
    Entry<Double, Double> entry2 = it2.next();

    while (true) {
      try {
        if (entry1.getKey() < entry2.getKey() - tolerance) // x1 < x2 - tolerance
          entry1 = it1.next();
        else if (entry2.getKey() < entry1.getKey() - tolerance) // x2 < x1 - tolerance
          entry2 = it2.next();
        else { // |x1 - x2| < tolerance
          result += entry1.getValue() * entry2.getValue();
          entry1 = it1.next();
          entry2 = it2.next();
        }
      } catch (NoSuchElementException e) {
        break;
      }
    }

    return result;
  }

  /**
   * <p>
   * discrete_dot_product.
   * </p>
   *
   * @param f1 a {@link Map} object.
   * @param f2 a {@link Map} object.
   * @param tolerance a double.
   * @return a double.
   */
  static public double discrete_dot_product(final Map<Double, Double> f1,
      final Map<Double, Double> f2, double tolerance) {
    return discrete_dot_product(new TreeMap<Double, Double>(f1), new TreeMap<Double, Double>(f2),
        tolerance);
  }

  /**
   * Find derivative of func using two-sided rule for the central points and one-sided rule for the
   * end-points
   *
   * @param func a {@link NavigableMap} object.
   * @return a {@link NavigableMap} object.
   */
  static public NavigableMap<Double, Double> differentiate(
      final NavigableMap<Double, Double> func) {
    NavigableMap<Double, Double> result = new TreeMap<>();

    if (func.size() < 3)
      return result;

    Iterator<Entry<Double, Double>> it = func.entrySet().iterator();
    Entry<Double, Double> center = it.next();
    Entry<Double, Double> right = it.next();

    double leftX, leftY;
    double centerX = center.getKey(), centerY = center.getValue();
    double rightX = right.getKey(), rightY = right.getValue();

    // Right-hand side derivative at the first entry
    result.put(centerX, (rightY - centerY) / (rightX - centerX));

    while (it.hasNext()) {
      Entry<Double, Double> entry = it.next();

      leftX = centerX;
      leftY = centerY;
      centerX = rightX;
      centerY = rightY;
      rightX = entry.getKey();
      rightY = entry.getValue();

      // Two-side derivative at the central entries
      result.put(centerX, (rightY - leftY) / (rightX - leftX));
    }

    // Left-hand side derivative at the last entry
    result.put(rightX, (rightY - centerY) / (rightX - centerX));

    return result;
  }

  /**
   * Convolution of two functions
   *
   * @param f1 a {@link NavigableMap} object.
   * @param f2 a {@link NavigableMap} object.
   * @param shift a double.
   * @return a double.
   */
  static public double convolution(final NavigableMap<Double, Double> f1,
      final NavigableMap<Double, Double> f2, final double shift) {
    NavigableMap<Double, Double> shifted2 = new TreeMap<>();

    for (final Entry<Double, Double> entry : f2.entrySet())
      shifted2.put(entry.getKey() + shift, entry.getValue());

    return continuous_dot_product(f1, shifted2);
  }

  /**
   * Multiplies function by scale
   *
   * @param func a {@link NavigableMap} object.
   * @param scale a double.
   * @return a {@link NavigableMap} object.
   */
  static public NavigableMap<Double, Double> scale(final NavigableMap<Double, Double> func,
      final double scale) {
    NavigableMap<Double, Double> result = new TreeMap<>();

    for (Entry<Double, Double> entry : func.entrySet())
      result.put(entry.getKey(), scale * entry.getValue());

    return result;
  }

  /**
   * <p>
   * linearCombination.
   * </p>
   *
   * @param functions a {@link List} object.
   * @param coefficients an array of double.
   * @return a {@link NavigableMap} object.
   */
  static public NavigableMap<Double, Double> linearCombination(
      final List<NavigableMap<Double, Double>> functions, final double[] coefficients) {
    NavigableMap<Double, Double> result = new TreeMap<>();

    int size = Integer.min(functions.size(), coefficients.length);

    for (int i = 0; i < size; ++i) {
      for (Entry<Double, Double> e : functions.get(i).entrySet()) {
        double key = e.getKey();
        double value = coefficients[i] * e.getValue();
        result.put(key, result.getOrDefault(key, 0.0) + value);
      }
    }

    return result;
  }
}
