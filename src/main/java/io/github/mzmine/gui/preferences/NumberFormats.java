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

package io.github.mzmine.gui.preferences;

import java.text.NumberFormat;
import org.jetbrains.annotations.Nullable;

public record NumberFormats(NumberFormat mzFormat, NumberFormat rtFormat,
                            NumberFormat mobilityFormat, NumberFormat ccsFormat,
                            NumberFormat intensityFormat, NumberFormat ppmFormat,
                            NumberFormat percentFormat, NumberFormat scoreFormat,
                            UnitFormat unitFormat) {

  private static final String empty = "";
  public String mz(double mz) {
    return mzFormat.format(mz);
  }

  public String mz(@Nullable Number mz) {
    if(mz == null) {
      return empty;
    }
    return mzFormat.format(mz);
  }

  public String rt(float rt) {
    return rtFormat.format(rt);
  }

  public String rt(@Nullable Number rt) {
    if(rt == null) {
      return empty;
    }
    return rtFormat.format(rt);
  }

  public String mobility(float mobility) {
    return mobilityFormat.format(mobility);
  }

  public String mobility(double mobility) {
    return mobilityFormat.format(mobility);
  }

  public String mobility(@Nullable Number mobility) {
    if(mobility == null) {
      return empty;
    }
    return mobilityFormat.format(mobility);
  }

  public String ccs(float ccs) {
    return ccsFormat.format(ccs);
  }

  public String ccs(@Nullable Number ccs) {
    if(ccs == null) {
      return empty;
    }
    return ccsFormat.format(ccs);
  }

  public String intensity(double intensity) {
    return intensityFormat.format(intensity);
  }

  public String intensity(@Nullable Number intensity) {
    if(intensity == null) {
      return empty;
    }
    return intensityFormat.format(intensity);
  }

  public String ppm(double ppm) {
    return ppmFormat.format(ppm);
  }

  public String ppm(@Nullable Number ppm) {
    if(ppm == null) {
      return empty;
    }
    return ppmFormat.format(ppm);
  }

  public String percent(double percent) {
    return ppmFormat.format(percent);
  }

  public String percent(@Nullable Number percent) {
    if(percent == null) {
      return empty;
    }
    return percentFormat.format(percent);
  }

  public String score(double score) {
    return scoreFormat.format(score);
  }

  public String score(@Nullable Number score) {
    if(score == null) {
      return empty;
    }
    return scoreFormat.format(score);
  }

  public String unit(String label, String unit) {
    return unitFormat.format(label, unit);
  }
}
