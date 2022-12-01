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

package io.github.mzmine.modules.io.projectload.version_2_5;

enum RawDataElementName_2_5 {
  
  RAWDATA("rawdata"), //
  NAME("name"), //
  QUANTITY_SCAN("num_scans"), //
  ID("id"), //
  SCAN("scan"), //
  SCAN_ID("id"), //
  MS_LEVEL("mslevel"), //
  QUANTITY_FRAGMENT_SCAN("fragmentscans"), //
  FRAGMENT_SCAN("fragmentscan"), //
  QUANTITY("quantity"), //
  PARENT_SCAN("parent"), //
  PRECURSOR_MZ("precursor_mz"), //
  PRECURSOR_CHARGE("precursor_charge"), //
  RETENTION_TIME("rt"), //
  CENTROIDED("centroid"), //
  QUANTITY_DATAPOINTS("num_dp"), //
  MASS_LIST("mass_list"), //
  STORED_DATAPOINTS("stored_datapoints"), //
  STORED_DATA("stored_data"), //
  STORAGE_ID("storage_id"), //
  POLARITY("polarity"), //
  SCAN_DESCRIPTION("scan_description"), //
  SCAN_MZ_RANGE("scan_mz_range"), //
  ION_MOBILITY("mobility");

  private String elementName;

  private RawDataElementName_2_5(String itemName) {
    this.elementName = itemName;
  }

  public String getElementName() {
    return elementName;
  }

}
