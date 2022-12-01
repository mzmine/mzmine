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

package io.github.mzmine.modules.io.projectload.version_3_0;

public class CONST {

  private CONST() {
  }

  /**
   * Convenience constant to save null values that shall stay null.
   */
  public static final String XML_NULL_VALUE = "NULL_VALUE";

  /**
   * Scan stuff
   */
  public static final String XML_MZ_VALUES_ELEMENT = "mzs";
  public static final String XML_INTENSITY_VALUES_ELEMENT = "intensities";
  public static final String XML_MOBILITY_VALUES_ELEMENT = "mobilities";
  public static final String XML_RAW_FILES_LIST_ELEMENT = "rawdatafiles";
  public static final String XML_RAW_FILE_ELEMENT = "rawdatafile";
  public static final String XML_RAW_FILE_NAME_ELEMENT = "name";
  public static final String XML_RAW_FILE_PATH_ELEMENT = "path";
  public static final String XML_RAW_FILE_SCAN_INDEX_ATTR = "scanindex";
  public static final String XML_RAW_FILE_SCAN_ELEMENT = "scan";
  public static final String XML_SCAN_DEF_ATTR = "scandefinition";
  public static final String XML_MSLEVEL_ATTR = "mslevel";
  public static final String XML_CE_ATTR = "ce";
  public static final String XML_RT_ATTR = "rt";
  public static final String XML_PRECURSOR_MZ_ATTR = "precursormz";
  public static final String XML_PRECURSOR_CHARGE_ATTR = "precursorcharge";
  public static final String XML_INTENSITY_MERGE_TYPE_ATTR = "mergingtype";
  public static final String XML_SCAN_LIST_ELEMENT = "scans";
  public static final String XML_POLARITY_ATTR = "polarity";

  public static final String XML_MERGE_TYPE_ATTR = "merge_spec_type";
  /**
   * General
   */
  public static final String XML_NUM_VALUES_ATTR = "numvalues";

  /**
   * Feature list stoff
   */
  public static final String XML_NUM_ROWS_ATTR = "numberofrows";
  public static final String XML_FLIST_NAME_ATTR = "featurelistname";
  public static final String XML_DATE_CREATED_ATTR = "date";
  public static final String XML_DATA_TYPE_ELEMENT = "datatype";
  public static final String XML_DATA_TYPE_ID_ATTR = "type";
  public static final String XML_FEATURE_ELEMENT = "feature";
  public static final String XML_ROW_ELEMENT = "row";
  public static final String XML_FEATURE_LIST_ELEMENT = "featurelist";
  public static final String XML_ROOT_ELEMENT = "root";

  public static final String XML_FLIST_METADATA_ELEMENT = "metadata";
  public static final String XML_FLIST_DATE_CREATED_ELEMENT = "date";
  public static final String XML_FLIST_NAME_ELEMENT = "name";
  public static final String XML_FLIST_SELECTED_SCANS_ELEMENT = "selectedscans";
  public static final String XML_FLIST_APPLIED_METHOD_ELEMENT = "appliedmethod";
  public static final String XML_FLIST_APPLIED_METHODS_LIST_ELEMENT = "appliedmethodslist";

}
