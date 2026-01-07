/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_online_reactivity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import java.util.ArrayList;
import java.util.List;

/**
 * A reaction that can be loaded from csv/tsv or json
 *
 * @param reactionName     the name of a reaction
 * @param filenameContains raw data file names will be filtered to contain this sub string (case
 *                         insensitive)
 * @param eductSmarts
 * @param reactionSmarts
 * @param deltaMz
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public record OnlineReaction(String reactionName, String filenameContains, String eductSmarts,
                             String reactionSmarts, double deltaMz) {

  /**
   * Creates all adduct reactions where educt and product have different adducts so the mass is
   * different.
   *
   * @param eductAdducts   adduct selection of educt
   * @param productAdducts adduct selection of product
   * @return list that contains all adduct crossover reactions - does not include the initial
   * reaction
   */
  public List<OnlineReaction> createCrossAdductReactions(final List<IonModification> eductAdducts,
      final List<IonModification> productAdducts) {
    List<OnlineReaction> reactions = new ArrayList<>();
    for (final IonModification eductAdduct : eductAdducts) {
      for (final IonModification productAdduct : productAdducts) {
        if (eductAdduct.equals(productAdduct)) {
          continue;
        }
        String suffix = ": " + eductAdduct.toString(false) + " → " + productAdduct.toString(false);
        double deltaMz = productAdduct.getMass() - eductAdduct.getMass();
        reactions.add(this.withNameSuffix(suffix, deltaMz));
      }
    }
    return reactions;
  }

  /**
   * @param suffix     added to the name
   * @param changeToMz added to the deltaMz of this reaction
   * @return new reaction with changed name and delta mz
   */
  public OnlineReaction withNameSuffix(final String suffix, final double changeToMz) {
    return new OnlineReaction(reactionName + suffix, filenameContains, eductSmarts, reactionSmarts,
        deltaMz + changeToMz);
  }

  public enum Type {
    Reaction, Educt, Product;
  }
}
