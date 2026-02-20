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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.DataType;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Dedicated list type to keep MZconnect analog-search results separate from standard compound DB
 * annotations.
 */
public class MzConnectAnalogCompoundDatabaseMatchesType extends CompoundDatabaseMatchesType {

  private static final List<DataType> subTypes = createSubTypes();

  private static List<DataType> createSubTypes() {
    final List<DataType> types = new ArrayList<>();
    types.add(new MzConnectAnalogCompoundDatabaseMatchesType());
    for (DataType dataType : CompoundDatabaseMatchesType.subTypes) {
      if (!(dataType instanceof CompoundDatabaseMatchesType)) {
        types.add(dataType);
      }
    }
    return List.copyOf(types);
  }

  @Override
  public @NotNull String getUniqueID() {
    return "compound_db_identity_mzconnect_analog";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Compound DB analogs";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
