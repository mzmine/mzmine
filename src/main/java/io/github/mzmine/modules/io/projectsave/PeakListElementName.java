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

package io.github.mzmine.modules.io.projectsave;

enum PeakListElementName {

  PEAKLIST("peaklist"), //
  PEAKLIST_DATE("created"), //
  QUANTITY("quantity"), //
  RAWFILE("raw_file"), //
  PEAKLIST_NAME("pl_name"), //
  ID("id"), //
  RT("rt"), //
  MZ("mz"), //
  HEIGHT("height"), //
  RTRANGE("rt_range"), //
  MZRANGE("mz_range"), //
  AREA("area"), //
  STATUS("status"), //
  COLUMN("column_id"), //
  SCAN_ID("scan_id"), //
  ROW("row"), //
  PEAK_INFORMATION("information"), //
  INFO_PROPERTY("information_property"), //
  PEAK_IDENTITY("identity"), //
  PREFERRED("preferred"), //
  IDPROPERTY("identity_property"), //
  NAME("name"), //
  COMMENT("comment"), //
  PEAK("peak"), //
  ISOTOPE_PATTERN("isotope_pattern"), //
  DESCRIPTION("description"), //
  CHARGE("charge"), //
  ISOTOPE("isotope"), //
  MZPEAKS("mzpeaks"), //
  METHOD("applied_method"), //
  METHOD_NAME("method_name"), //
  METHOD_PARAMETERS("method_parameters"), //
  REPRESENTATIVE_SCAN("best_scan"), //
  FRAGMENT_SCAN("fragment_scan"), //
  ALL_MS2_FRAGMENT_SCANS("all_MS2_fragment_scans"), //
  INDEX("index"), //
  SHARPNESS("sharpness"), //
  PARENT_CHROMATOGRAM_ROW_ID("parent_chromatogram_row_id");

  private String elementName;

  private PeakListElementName(String itemName) {
    this.elementName = itemName;
  }

  public String getElementName() {
    return elementName;
  }

}
