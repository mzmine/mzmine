/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.InternalTypedRowsRelationship;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An online reaction educt -> product relationship. Can be visualized in network
 */
public class OnlineReactionMatch extends InternalTypedRowsRelationship {

  private final OnlineReaction reaction;

  // internally a.getID < b.getID so they may be swapped
  private final boolean isSwappedAB;
  private final OnlineReaction.Type typeOfThisRow;

  /**
   * @param educt         the educt row
   * @param product       the product row
   * @param reaction      the reaction
   * @param typeOfThisRow the type of the row that this match was added to
   */
  public OnlineReactionMatch(final FeatureListRow educt, final FeatureListRow product,
      OnlineReaction reaction, OnlineReaction.Type typeOfThisRow) {
    super(educt, product, Type.ONLINE_REACTION);
    this.reaction = reaction;
    isSwappedAB = getRowA() != educt;
    this.typeOfThisRow = typeOfThisRow;
  }

  @Override
  public double getScore() {
    return 1;
  }

  @Override
  public @NotNull String getAnnotation() {
    return toString();
  }

  public OnlineReaction getReaction() {
    return reaction;
  }

  /**
   * The type of the row this match was added to. Needed for visualization in table
   */
  public OnlineReaction.Type getTypeOfThisRow() {
    return typeOfThisRow;
  }

  /**
   * Get the type of the input row
   *
   * @return either educt or product of a reaction
   */
  @NotNull
  public OnlineReaction.Type getTypeOfRow(FeatureListRow row) {
    // not swapped then A is educt otherwise B is educt
    boolean isA = Objects.equals(row, getRowA());
    if (isA) {
      return isSwappedAB ? OnlineReaction.Type.Product : OnlineReaction.Type.Educt;
    } else {
      return !isSwappedAB ? OnlineReaction.Type.Product : OnlineReaction.Type.Educt;
    }
  }

  @NotNull
  public FeatureListRow getEductRow() {
    return isSwappedAB ? getRowB() : getRowA();
  }

  @NotNull
  public FeatureListRow getProductRow() {
    return isSwappedAB ? getRowA() : getRowB();
  }

  @Nullable
  public FeatureListRow getRow(OnlineReaction.Type type) {
    return switch (type) {
      case Reaction -> null;
      case Educt -> getEductRow();
      case Product -> getProductRow();
    };
  }

  @Override
  public String toString() {
    NumberFormat mzFormat = MZmineCore.getConfiguration().getGuiFormats().mzFormat();
    return "%s Δm/z: %s".formatted(reaction.reactionName(), mzFormat.format(reaction.deltaMz()));
  }


  public String toFullString() {
    NumberFormat mzFormat = MZmineCore.getConfiguration().getGuiFormats().mzFormat();
    return "%s Educt: %s Reaction: %s Δm/z: %s".formatted(reaction.reactionName(),
        reaction.eductSmarts(), reaction.reactionSmarts(), mzFormat.format(reaction.deltaMz()));
  }

  public int getPartnerRowId() {
    return getRowB().getID();
  }
}
