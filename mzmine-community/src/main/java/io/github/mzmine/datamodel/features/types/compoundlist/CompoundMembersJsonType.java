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

package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMembers;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Mapped linked type so that exports this simple json string for compound members as the
/// {@link CompoundMembersType} does not export a column as it is a substype type.
public class CompoundMembersJsonType extends StringType implements MappingType<String> {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_members_json";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Members json";
  }

  @Override
  public @Nullable String getValue(@NotNull ModularDataModel model) {
    final CompoundMembers members = model.get(CompoundMembersType.class);
    if (members == null) {
      return null;
    }

    return members.toSimpleJson();
  }
}
