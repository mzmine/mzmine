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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling;

import com.google.common.collect.Range;

import java.util.Collections;
import java.util.List;

/**
 * Class containing methods for extracting subsets of distributions
 * <p>
 * used with (single dimensional) distributions of all mass measurement errors to extract certain subsets
 * that are considered substantial to later estimation of measurement bias
 * <p>
 * after assigning all measured m/z peaks with their predicted true m/z value
 * (eg: by matching against lists of standard calibrants or using molecular formula assignment)
 * we can obtain a distribution of errors coming from these measurements,
 * however usually not all of these matches are correct so not all of these errors are actually substantial
 * to estimation of systematic error of the measurements,
 * so to obtain more accurate bias estimates, we can first extract certain subsets from the distribution that
 * are more likely corresponding to correct matches and so important for the bias estimation
 * <p>
 * we can approximate the extraction by taking high density error ranges, modelling error values vs m/z values,
 * filtering outliers and so on
 * <p>
 * currently clustering methods are implemented that take a subset (range) of certain max size with most items in it,
 * or cluster the items by distance between items and return cluster with most items in it
 */
public class DistributionExtractor {
  /**
   * Returns a range from the distribution that contains most items in it and has certain max length
   *
   * @param items          distribution items to extract, eg: mass measurement errors
   * @param rangeMaxLength max length of the range
   * @return distribution range object, with extracted items and range data
   */
  public static DistributionRange fixedLengthRange(List<Double> items, double rangeMaxLength) {
    if (items.isEmpty()) {
      throw new IllegalArgumentException("Empty items list");
    }
    if (rangeMaxLength <= 0) {
      throw new IllegalArgumentException("Non-positive range max length");
    }

    Collections.sort(items);
    int endIndex = 0;
    int mostItemsStart = 0;
    int mostItemsEnd = 0;
    for (int startIndex = 0; startIndex < items.size(); startIndex++) {
      while (endIndex + 1 < items.size() && items.get(endIndex + 1) - items.get(startIndex) <= rangeMaxLength) {
        endIndex++;
      }
      if (endIndex - startIndex > mostItemsEnd - mostItemsStart) {
        mostItemsStart = startIndex;
        mostItemsEnd = endIndex;
      }
    }

    return buildDistributionRange(items, mostItemsStart, mostItemsEnd);
  }

  /**
   * Extend range by including subsequent items within given distance tolerance
   *
   * @param range     extracted range
   * @param tolerance distance tolerance
   * @return distribution range object with extended size
   */
  public static DistributionRange fixedToleranceExtensionRange(DistributionRange range, double tolerance) {
    var items = range.getOriginalItems();
    if (tolerance < 0) {
      throw new IllegalArgumentException("Negative distance tolerance");
    }

    int mostItemsStart = range.getIndexRange().lowerEndpoint();
    int mostItemsEnd = range.getIndexRange().upperEndpoint();

    while ((mostItemsStart > 0 && items.get(mostItemsStart) - items.get(mostItemsStart - 1) <= tolerance)
            || (mostItemsEnd < items.size() - 1 && items.get(mostItemsEnd + 1) - items.get(mostItemsEnd) <= tolerance)) {
      if (mostItemsStart > 0 && items.get(mostItemsStart) - items.get(mostItemsStart - 1) <= tolerance) {
        mostItemsStart--;
      }
      if (mostItemsEnd < items.size() - 1 && items.get(mostItemsEnd + 1) - items.get(mostItemsEnd) <= tolerance) {
        mostItemsEnd++;
      }

    }

    return buildDistributionRange(items, mostItemsStart, mostItemsEnd);
  }

  /**
   * Returns a most populated range from the distribution after splitting it into subranges by fixed tolerance
   * Given a list of items, split them into subranges by merging points that are within given tolerance of each other
   * This can be done by sorting the list, then iterating over it while keeping track of the longest subrange
   * that was considered,
   * at every iteration include the next point in the current range if it is within the tolerance of that range
   * (its distance to the last element is smaller than the tolerance)
   * or start a new range otherwise
   * This methods return the (first) range containing most points found in the process
   *
   * @param items     distribution items
   * @param tolerance distance tolerance between the points in the distribution
   * @return distribution range object
   */
  public static DistributionRange mostPopulatedRangeCluster(List<Double> items, double tolerance) {
    if (items.isEmpty()) {
      throw new IllegalArgumentException("Empty items list");
    }
    if (tolerance <= 0) {
      throw new IllegalArgumentException("Non-positive tolerance");
    }

    Collections.sort(items);
    int startIndex = 0;
    int endIndex = 0;
    int mostItemsStart = 0;
    int mostItemsEnd = 0;
    while (endIndex < items.size()) {
      while (endIndex + 1 < items.size() && items.get(endIndex + 1) - items.get(endIndex) <= tolerance) {
        endIndex++;
      }
      if (endIndex - startIndex > mostItemsEnd - mostItemsStart) {
        mostItemsStart = startIndex;
        mostItemsEnd = endIndex;
      }
      startIndex = endIndex + 1;
      endIndex = startIndex;
    }

    return buildDistributionRange(items, mostItemsStart, mostItemsEnd);
  }

  /**
   * Return distribution range made of items between given percentiles
   *
   * @param items
   * @param lowerPercentile
   * @param upperPercentile
   * @return
   */
  public static DistributionRange interpercentileRange(List<Double> items,
                                                       double lowerPercentile, double upperPercentile) {
    if (items.isEmpty()) {
      throw new IllegalArgumentException("Empty items list");
    }
    if (!(lowerPercentile >= 0 && lowerPercentile <= upperPercentile && upperPercentile <= 100)) {
      throw new IllegalArgumentException("Required 0 <= lowerPercentile <= upperPercentile <= 100");
    }

    int lowerIndex = Math.max((int) Math.ceil(lowerPercentile / 100 * items.size()) - 1, 0);
    int upperIndex = (int) Math.ceil(upperPercentile / 100 * items.size()) - 1;

    return buildDistributionRange(items, lowerIndex, upperIndex);
  }

  /**
   * Returns distribution range made of all items
   *
   * @param items distribution items
   * @return distribution range object
   */
  public static DistributionRange wholeRange(List<Double> items) {
    return buildDistributionRange(items, 0, items.size() - 1);
  }

  protected static DistributionRange buildDistributionRange(List<Double> items, int startIndex, int endIndex) {
    Range<Integer> indexRange = Range.closed(startIndex, endIndex);
    Range<Double> valueRange = Range.closed(items.get(startIndex), items.get(endIndex));
    List<Double> extractedItems = items.subList(startIndex, endIndex + 1);

    return new DistributionRange(items, extractedItems, indexRange, valueRange);
  }

}
