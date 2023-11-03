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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.RowGroup;
import java.util.ArrayList;
import java.util.List;

public class RowGroupSimple implements RowGroup {

  // running index of groups
  private final R2RMap<RowsRelationship> map;
  protected int groupID;
  protected List<FeatureListRow> rows;


  public RowGroupSimple(final int groupID, R2RMap<RowsRelationship> map) {
    this.groupID = groupID;
    this.map = map;
    rows = new ArrayList<>();
  }

  @Override
  public List<FeatureListRow> getRows() {
    return rows;
  }

  @Override
  public boolean add(final FeatureListRow e) {
    return rows.add(e);
  }

  @Override
  public int getGroupID() {
    return groupID;
  }

  @Override
  public void setGroupID(final int groupID) {
    this.groupID = groupID;
  }

  @Override
  public boolean isCorrelated(final int i, final int k) {
    return isCorrelated(get(i), get(k));
  }

  @Override
  public boolean isCorrelated(final FeatureListRow a, final FeatureListRow b) {
    return map.get(a, b) != null;
  }
}
