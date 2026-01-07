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

package io.github.mzmine.util.reporting.jasper;

import io.mzmine.reports.SingleColumnRow;
import io.mzmine.reports.TwoColumnRow;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Non thread-safe collection of figures for a report.
 */
public class FigureCollection {

  private final List<SingleColumnRow> singleColumnRows = new ArrayList<>();
  private final List<TwoColumnRow> twoColumnRows = new ArrayList<>();
  private FigureAndCaption tempFirstFigure = null;

  private boolean finished = false;

  public FigureCollection() {

  }

  public void addSingleFigureRow(@NotNull FigureAndCaption fig) {
    if (finished) {
      throw new IllegalStateException("Cannot add a new figure if results were already queried.");
    }
    singleColumnRows.add(new SingleColumnRow(fig.chartSvg(), fig.caption()));
  }

  /**
   * not thread safe
   *
   * @param fig
   */
  public void addTwoFigureRowFigure(@NotNull final FigureAndCaption fig) {
    if (finished) {
      throw new IllegalStateException("Cannot add a new figure if results were already queried.");
    }

    if (tempFirstFigure == null) {
      tempFirstFigure = fig;
      return;
    }

    twoColumnRows.add(
        new TwoColumnRow(tempFirstFigure.chartSvg(), tempFirstFigure.caption(), fig.chartSvg(),
            fig.caption()));
    tempFirstFigure = null;
  }

  public List<TwoColumnRow> getTwoFigureRows() {
    finished = true;

    if (tempFirstFigure != null) {
      // add potential last figure
      twoColumnRows.add(
          new TwoColumnRow(tempFirstFigure.chartSvg(), tempFirstFigure.caption(), null, null));
      tempFirstFigure = null;
    }
    return twoColumnRows;
  }

  public List<SingleColumnRow> getSingleFigureRows() {
    finished = true;
    return singleColumnRows;
  }
}
