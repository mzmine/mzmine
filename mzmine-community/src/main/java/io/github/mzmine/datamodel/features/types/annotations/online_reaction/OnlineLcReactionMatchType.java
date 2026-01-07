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

package io.github.mzmine.datamodel.features.types.annotations.online_reaction;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.TypeStringType;
import io.github.mzmine.datamodel.features.types.annotations.SmartsEductStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmartsReactionStructureType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReactionMatch;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OnlineLcReactionMatchType extends ListWithSubsType<OnlineReactionMatch> {

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of( //
      new OnlineLcReactionMatchType(), //
      new EductIdType(), //
      new ProductIdType(), //
      new TypeStringType(), //
      new MzAbsoluteDifferenceType(), //
      new SmartsEductStructureType(), //
      new SmartsReactionStructureType() //
  );


  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public <K> @Nullable K map(@NotNull final DataType<K> subType,
      final OnlineReactionMatch match) {
    OnlineReaction reaction = match.getReaction();
    return (K) switch (subType) {
      case OnlineLcReactionMatchType __ -> match;
      case EductIdType __ -> match.getEductRow().getID();
      case ProductIdType __ -> match.getProductRow().getID();
      case TypeStringType __ -> match.getTypeOfThisRow();
      case SmartsEductStructureType __ -> reaction.eductSmarts();
      case SmartsReactionStructureType __ -> reaction.reactionSmarts();
      case MzAbsoluteDifferenceType __ -> reaction.deltaMz();
      default -> throw new UnsupportedOperationException(
          "DataType %s is not covered in map".formatted(subType.toString()));
    };
  }


  @Override
  public @NotNull String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "online_reaction";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Online reaction";
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }

}
