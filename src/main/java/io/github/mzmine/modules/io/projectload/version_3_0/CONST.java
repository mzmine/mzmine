/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.projectload.version_3_0;

public class CONST {

  private CONST() {
  }

  /**
   * Convenience constant to save null values that shall stay null.
   */
  public static final String XML_NULL_VALUE = "NULL_VALUE";

  public static final String XML_MZ_VALUES_ELEMENT = "mzs";
  public static final String XML_INTENSITY_VALUES_ELEMENT = "intensities";
  public static final String XML_MOBILITY_VALUES_ELEMENT = "mobilities";
  public static final String XML_SCAN_LIST_ELEMENT = "scans";
  public static final String XML_NUM_VALUES_ATTR = "numvalues";

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

  public static final String XML_RAW_FILES_LIST_ELEMENT = "rawdatafiles";
  public static final String XML_RAW_FILE_ELEMENT = "rawdatafile";
  public static final String XML_RAW_FILE_NAME_ELEMENT = "name";
  public static final String XML_RAW_FILE_PATH_ELEMENT = "path";
  public static final String XML_RAW_FILE_SCAN_INDEX_ATTR = "scanindex";
  public static final String XML_RAW_FILE_SCAN_ELEMENT = "scan";
}
