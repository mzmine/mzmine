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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import java.util.Arrays;

/**
 * Used by datapointprocessing methods to differentiate between MS^1 and MS/MS. Also used to
 * dynamically load processing queues and create elements on the interface.
 * 
 * CAUTION: The method croppedValues() is used to dynamically create and load different processing
 * queues and everything regarding them on the interface. When adding new Types to this enum, make
 * sure MSANY stays the last one. MSANY is used by methods, that are applicable on any MS-level, but
 * it does not get it's own processing queue. This is why croppedValues() exists and cuts off the
 * last value of the values() method.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public enum MSLevel {
  MSONE("MS"), MSMS("MS/MS"), MSANY("MS-any");

  final String name;

  MSLevel(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static MSLevel[] cropValues() {
    MSLevel[] values = MSLevel.values();
    return Arrays.copyOf(values, values.length - 1);
  }
};
