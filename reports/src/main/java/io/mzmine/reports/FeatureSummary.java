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

package io.mzmine.reports;

public class FeatureSummary {

  private final String id;
  private final String mz;
  private final String rtInMinutes;
  private final String ccs;
  private final String height;
  private final String area;
  private final String compoundName;
  private final String identifier;
  private final String areaPercent;

  public FeatureSummary(String id, String mz, String rtInMinutes, String ccs, String height,
      String area, String compoundName, String identifier, String areaPercent) {
    this.id = id;
    this.mz = mz;
    this.rtInMinutes = rtInMinutes;
    this.ccs = ccs;
    this.height = height;
    this.area = area;
    this.compoundName = compoundName;
    this.identifier = identifier;
    this.areaPercent = areaPercent;
  }

  public String getId() {
    return id;
  }

  public String getMz() {
    return mz;
  }

  public String getRtInMinutes() {
    return rtInMinutes;
  }

  public String getCcs() {
    return ccs;
  }

  public String getHeight() {
    return height;
  }

  public String getArea() {
    return area;
  }

  public String getCompoundName() {
    return compoundName;
  }

  public String getIdentifier() {
    return identifier;
  }

  public String getAreaPercent() {
    return areaPercent;
  }
}
