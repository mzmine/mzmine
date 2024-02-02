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
import org.jetbrains.annotations.NotNull;

public class OnlineReactionMatch extends InternalTypedRowsRelationship {

  private final OnlineReaction reaction;
  private final OnlineReaction.Type typeOfRowA;

  public OnlineReactionMatch(final FeatureListRow a, final FeatureListRow b,
      OnlineReaction reaction, final OnlineReaction.Type typeOfRowA) {
    super(a, b, Type.ONLINE_REACTION);
    this.reaction = reaction;
    this.typeOfRowA = typeOfRowA;
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

  public OnlineReaction.Type getTypeOfRowA() {
    return typeOfRowA;
  }

  @Override
  public String toString() {
    NumberFormat mzFormat = MZmineCore.getConfiguration().getGuiFormats().mzFormat();
    return STR."\{reaction.reactionName()} \{typeOfRowA} Δm/z: \{mzFormat.format(
        reaction.deltaMz())}";
  }


  public String toFullString() {
    NumberFormat mzFormat = MZmineCore.getConfiguration().getGuiFormats().mzFormat();
    return STR."\{reaction.reactionName()} as \{typeOfRowA}. Educt: \{reaction.eductSmarts()} Reaction: \{reaction.reactionSmarts()} Δm/z: \{mzFormat.format(
        reaction.deltaMz())}";
  }

  public int getPartnerRowId() {
    return getRowB().getID();
  }
}
