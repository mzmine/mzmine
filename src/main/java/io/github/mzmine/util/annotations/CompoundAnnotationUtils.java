/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util.annotations;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CompoundAnnotationUtils {

  /**
   * A list of matches where each entry has a different compound name.
   *
   * @param matches can contain duplicate compound names - the method will find the best annotation
   *                for each compound name by sorting by least modified adduct and highest score
   * @return list of unique compound names
   */
  public static List<CompoundDBAnnotation> getBestMatchesPerCompoundName(
      final List<CompoundDBAnnotation> matches) {
    Map<String, List<CompoundDBAnnotation>> compoundsMap = new HashMap<>();

    // might have different adducts for the same compound - list them by compound name
    for (final CompoundDBAnnotation match : matches) {
      var list = compoundsMap.computeIfAbsent(match.getCompoundName(), s -> new ArrayList<>());
      list.add(match);
    }
    // sort by number of adducts + modifications
    var oneMatchPerCompound = compoundsMap.values().stream()
        .map(compound -> compound.stream().min(getSorterLeastModifiedCompoundFirst()).orElse(null))
        .filter(Objects::nonNull).toList();

    return oneMatchPerCompound;
  }

  /**
   * First sort by adduct type: simple IonType first, which means M+H better than 2M+H2+2 and
   * 2M-H2O+H+.
   *
   * @return sorter
   */
  public static Comparator<CompoundDBAnnotation> getSorterLeastModifiedCompoundFirst() {
    return Comparator.comparing(CompoundDBAnnotation::getAdductType,
            Comparator.nullsLast(Comparator.comparingInt(IonType::getTotalPartsCount)))
        .thenComparing(getSorterMaxScoreFirst());
  }

  /**
   * max score first, score descending
   *
   * @return sorter
   */
  public static Comparator<CompoundDBAnnotation> getSorterMaxScoreFirst() {
    return Comparator.comparing(CompoundDBAnnotation::getScore,
        Comparator.nullsLast(Comparator.reverseOrder()));
  }

}
