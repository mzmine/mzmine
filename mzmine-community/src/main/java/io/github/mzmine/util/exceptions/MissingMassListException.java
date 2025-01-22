/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.exceptions;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.ScanUtils;
import org.jetbrains.annotations.Nullable;

public class MissingMassListException extends RuntimeException {

  public MissingMassListException(Scan scan) {
    this("", scan);
  }

  public MissingMassListException(String message, @Nullable Scan scan) {
    super(
        "Missing mass list in scan " + (scan != null ? ScanUtils.scanToString(scan, true) : "null")
            + ". " + message);
  }

  /**
   * This constructor is only to be used when no direct scan object is present. (e.g.,
   * {@link io.github.mzmine.datamodel.impl.MobilityScanStorage}) Use other constructors by
   * default.
   *
   * @param message Error message
   */
  public MissingMassListException(String message) {
    super(message);
  }

}
