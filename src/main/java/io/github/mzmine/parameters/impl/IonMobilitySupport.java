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

package io.github.mzmine.parameters.impl;

public enum IonMobilitySupport {
  /**
   * Module requires mobility data to work.
   */
  ONLY,

  /**
   * Module can be used to process non-ims and ims data.
   */
  SUPPORTED,

  /**
   * Module has not been tested with ion mobility data, but should work.
   */
  UNTESTED,

  /**
   * Module has been tested with ion mobility data and shows certain restrictions when processing
   * that data. A specific warning message should be displayed by overriding {@link
   * SimpleParameterSet#getRestrictedIonMobilitySupportMessage()}.
   */
  RESTRICTED,

  /**
   * Module does not support ion mobility data and will produce wrong results.
   */
  UNSUPPORTED,
}
