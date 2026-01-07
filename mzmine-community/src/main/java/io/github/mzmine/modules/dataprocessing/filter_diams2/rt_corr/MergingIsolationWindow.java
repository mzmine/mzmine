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

package io.github.mzmine.modules.dataprocessing.filter_diams2.rt_corr;

import io.github.mzmine.datamodel.Scan;
import java.util.List;
import org.jetbrains.annotations.NotNull;

final class MergingIsolationWindow {

  private final @NotNull List<Scan> scans;
  private IsolationWindow window;

  MergingIsolationWindow(IsolationWindow window, @NotNull List<Scan> scans) {
    this.window = window;
    this.scans = scans;
  }

  IsolationWindow window() {
    return window;
  }

  @NotNull List<Scan> scans() {
    return scans;
  }

  void setWindow(IsolationWindow window) {
    this.window = window;
  }

  @Override
  public String toString() {
    return "%s (%s scans)".formatted(window.toString(), scans.size());
  }
}
