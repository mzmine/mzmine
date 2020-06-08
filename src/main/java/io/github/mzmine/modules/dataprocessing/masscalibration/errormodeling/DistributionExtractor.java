/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration.errormodeling;

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
    if (tolerance <= 0) {
      throw new IllegalArgumentException("Non-positive distance tolerance");
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

  protected static DistributionRange buildDistributionRange(List<Double> items, int startIndex, int endIndex){
    Range<Integer> indexRange = Range.closed(startIndex, endIndex);
    Range<Double> valueRange = Range.closed(items.get(startIndex), items.get(endIndex));
    List<Double> extractedItems = items.subList(startIndex, endIndex + 1);

    return new DistributionRange(items, extractedItems, indexRange, valueRange);
  }
}