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

package io.github.mzmine.gui.framework.fx;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * a JavaFX interface that can handle changes in selected rows, update the content based on the
 * rows
 */
public interface FeatureRowInterfaceFx {

  /**
   * Defines if this JavaFX node has row specific content like annotations, charts, etc
   *
   * @return true if content available
   */
  boolean hasContent();

  /**
   * Defines if this JavaFX node has NO row specific content like annotations, charts, etc
   *
   * @return true if NO content available; opposite of {@link #hasContent()}
   */
  default boolean isEmptyContent() {
    return !hasContent();
  }

  /**
   * Set the selected rows and update this interface
   *
   * @param selectedRows new list of rows - the old is replaced
   */
  void setFeatureRows(final @NotNull List<? extends FeatureListRow> selectedRows);

}
