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
import java.text.MessageFormat;

/**
 * A relationship that stores both rows
 */
public abstract class AbstractRowsRelationship implements RowsRelationship {

  private final FeatureListRow a;
  private final FeatureListRow b;

  public AbstractRowsRelationship(FeatureListRow a, FeatureListRow b) {
    if (a.getID() < b.getID()) {
      this.a = a;
      this.b = b;
    } else {
      this.b = a;
      this.a = b;
    }
  }

  @Override
  public FeatureListRow getRowA() {
    return a;
  }

  @Override
  public FeatureListRow getRowB() {
    return b;
  }

  @Override
  public String toString() {
    return MessageFormat
        .format("{0}'{'rowA={1}, rowB={2}, Score={3}'}'", this.getClass().getName(), a.getID(),
            b.getID(), getScoreFormatted());
  }
}
