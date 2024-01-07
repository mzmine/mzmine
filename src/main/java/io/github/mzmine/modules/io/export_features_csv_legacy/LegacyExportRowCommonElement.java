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

package io.github.mzmine.modules.io.export_features_csv_legacy;

public enum LegacyExportRowCommonElement {

  ROW_ID("Export row ID"), //
  ROW_MZ("Export row m/z"), //
  ROW_RT("Export row retention time"), //
  ROW_ION_MOBILITY("Export row ion mobility"), //
  ROW_ION_MOBILITY_UNIT("Export row ion mobility unit"), //
  ROW_CCS("Export row CCS"), //
  ROW_IDENTITY("Export row identity (main ID)"), //
  ROW_IDENTITY_ALL("Export row identity (all IDs)"), //
  ROW_IDENTITY_DETAILS("Export row identity (main ID + details)"), //
  ROW_COMMENT("Export row comment"), //
  ROW_FEATURE_NUMBER("Export row number of detected features"), //
  ROW_CORR_GROUP_ID("Export correlation group ID"), //
  ROW_MOL_NETWORK_ID("Export annotation network number"), //
  ROW_BEST_ANNOTATION("Export best ion annotation"), //
  ROW_BEST_ANNOTATION_AND_SUPPORT("Export best ion annotation (+support)"), //
  ROW_NEUTRAL_MASS("Export neutral M mass");

  private final String name;

  LegacyExportRowCommonElement(String name) {
    this.name = name;
  }

  public String toString() {
    return this.name;
  }
}
