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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.util.collections.IndexRangesList;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.Nullable;

/**
 * Text field component for an {@link IndexRangesList}. The user enters a comma separated list of
 * single indices and ranges, e.g. "1,5-6", which is parsed via
 * {@link IndexRangesList#parse(String)}.
 */
public class IndexRangesComponent extends TextField {

  public IndexRangesComponent() {
    super();
    setPrefColumnCount(12);
    setPromptText("1,5-6");
  }

  /**
   * @return the parsed ranges, an empty list for blank input, or {@code null} if the text cannot be
   * parsed into index ranges
   */
  public @Nullable IndexRangesList getValue() {
    try {
      return IndexRangesList.parse(getText());
    } catch (IllegalArgumentException e) {
      // null signals an unparsable input so the parameter can report it in checkValue
      return null;
    }
  }

  public void setValue(@Nullable final IndexRangesList ranges) {
    setText(ranges == null ? "" : ranges.asString());
  }
}
