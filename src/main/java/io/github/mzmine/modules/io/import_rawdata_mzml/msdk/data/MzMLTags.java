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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

/**
 * <p>
 * Class containing String constants of various XML tags and attributes found in the mzML format
 * </p>
 */
public abstract class MzMLTags {

  /** Constant <code>TAG_INDEXED_MZML="indexedmzML"</code> */
  public static final String TAG_INDEXED_MZML = "indexedmzML";
  /** Constant <code>TAG_MZML="mzML"</code> */
  public static final String TAG_MZML = "mzML";
  /** Constant <code>TAG_CV_LIST="cvList"</code> */
  public static final String TAG_CV_LIST = "cvList";
  /** Constant <code>TAG_DATA_PROCESSING_LIST="dataProcessingList"</code> */
  public static final String TAG_DATA_PROCESSING_LIST = "dataProcessingList";
  /** Constant <code>TAG_DATA_PROCESSING="dataProcessing"</code> */
  public static final String TAG_DATA_PROCESSING = "dataProcessing";
  /**
   * Constant <code>TAG_PROCESSING_METHOD="processingMethod"</code>
   */
  public static final String TAG_PROCESSING_METHOD = "processingMethod";
  /**
   * Constant <code>TAG_RUN="run"</code>
   */
  public static final String TAG_RUN = "run";
  /**
   * Constant <code>TAG_SPECTRUM_LIST="spectrumList"</code>
   */
  public static final String TAG_SPECTRUM_LIST = "spectrumList";
  /**
   * Constant <code>TAG_SPECTRUM="spectrum"</code>
   */
  public static final String TAG_SPECTRUM = "spectrum";
  /**
   * Constant <code>TAG_CV_PARAM="cvParam"</code>
   */
  public static final String TAG_CV_PARAM = "cvParam";
  /**
   * Constant <code>TAG_USER_PARAM="userParam"</code>
   */
  public static final String TAG_USER_PARAM = "userParam";
  /**
   * Constant <code>TAG_SCAN_LIST="scanList"</code>
   */
  public static final String TAG_SCAN_LIST = "scanList";
  /**
   * Constant <code>TAG_SCAN="scan"</code>
   */
  public static final String TAG_SCAN = "scan";
  /**
   * Constant <code>TAG_SCAN_WINDOW_LIST="scanWindowList"</code>
   */
  public static final String TAG_SCAN_WINDOW_LIST = "scanWindowList";
  /**
   * Constant <code>TAG_SCAN_WINDOW="scanWindow"</code>
   */
  public static final String TAG_SCAN_WINDOW = "scanWindow";
  /**
   * Constant <code>TAG_BINARY_DATA_ARRAY_LIST="binaryDataArrayList"</code>
   */
  public static final String TAG_BINARY_DATA_ARRAY_LIST = "binaryDataArrayList";
  /** Constant <code>TAG_BINARY_DATA_ARRAY="binaryDataArray"</code> */
  public static final String TAG_BINARY_DATA_ARRAY = "binaryDataArray";
  /** Constant <code>TAG_BINARY="binary"</code> */
  public static final String TAG_BINARY = "binary";
  /** Constant <code>TAG_CHROMATOGRAM_LIST="chromatogramList"</code> */
  public static final String TAG_CHROMATOGRAM_LIST = "chromatogramList";
  /** Constant <code>TAG_CHROMATOGRAM="chromatogram"</code> */
  public static final String TAG_CHROMATOGRAM = "chromatogram";
  /** Constant <code>TAG_PRECURSOR="precursor"</code> */
  public static final String TAG_PRECURSOR = "precursor";
  /** Constant <code>TAG_ISOLATION_WINDOW="isolationWindow"</code> */
  public static final String TAG_ISOLATION_WINDOW = "isolationWindow";
  /** Constant <code>TAG_ACTIVATION="activation"</code> */
  public static final String TAG_ACTIVATION = "activation";
  /** Constant <code>TAG_PRODUCT="product"</code> */
  public static final String TAG_PRODUCT = "product";
  /** Constant <code>TAG_PRODUCT_LIST="productList"</code> */
  public static final String TAG_PRODUCT_LIST = "productList";
  /** Constant <code>TAG_REF_PARAM_GROUP="referenceableParamGroup"</code> */
  public final static String TAG_REF_PARAM_GROUP = "referenceableParamGroup";
  /** Constant <code>TAG_REF_PARAM_GROUP_REF="referenceableParamGroupRef"</code> */
  public final static String TAG_REF_PARAM_GROUP_REF = "referenceableParamGroupRef";
  /** Constant <code>TAG_REF_PARAM_GROUP_LIST="referenceableParamGroupList"</code> */
  public final static String TAG_REF_PARAM_GROUP_LIST = "referenceableParamGroupList";
  /** Constant <code>TAG_PRECURSOR_LIST="precursorList"</code> */
  public final static String TAG_PRECURSOR_LIST = "precursorList";
  /** Constant <code>TAG_SELECTED_ION_LIST="selectedIonList"</code> */
  public final static String TAG_SELECTED_ION_LIST = "selectedIonList";
  /** Constant <code>TAG_SELECTED_ION="selectedIon"</code> */
  public final static String TAG_SELECTED_ION = "selectedIon";
  /** Constant <code>TAG_INDEX_LIST="indexList"</code> */
  public final static String TAG_INDEX_LIST = "indexList";
  /** Constant <code>TAG_INDEX="index"</code> */
  public final static String TAG_INDEX = "index";
  /** Constant <code>TAG_OFFSET="offset"</code> */
  public final static String TAG_OFFSET = "offset";
  /** Constant <code>TAG_INDEX_LIST_OFFSET="indexListOffset"</code> */
  public final static String TAG_INDEX_LIST_OFFSET = "indexListOffset";
  /** Constant <code>TAG_FILE_CHECKSUM="fileChecksum"</code> */
  public final static String TAG_FILE_CHECKSUM = "fileChecksum";

  /** Constant <code>ATTR_XSI="xsi"</code> */
  public static final String ATTR_XSI = "xsi";
  /** Constant <code>ATTR_SCHEME_LOCATION="schemeLocation"</code> */
  public static final String ATTR_SCHEME_LOCATION = "schemeLocation";
  /** Constant <code>ATTR_ID="id"</code> */
  public static final String ATTR_ID = "id";
  /** Constant <code>ATTR_VERSION="version"</code> */
  public static final String ATTR_VERSION = "version";
  /** Constant <code>ATTR_COUNT="count"</code> */
  public static final String ATTR_COUNT = "count";
  /** Constant <code>ATTR_SOFTWARE_REF="softwareRef"</code> */
  public static final String ATTR_SOFTWARE_REF = "softwareRef";
  /** Constant <code>ATTR_ORDER="order"</code> */
  public static final String ATTR_ORDER = "order";
  /** Constant <code>ATTR_DEFAULT_ARRAY_LENGTH="defaultArrayLength"</code> */
  public static final String ATTR_DEFAULT_ARRAY_LENGTH = "defaultArrayLength";
  /** Constant <code>ATTR_INDEX="index"</code> */
  public static final String ATTR_INDEX = "index";
  /** Constant <code>ATTR_CV_REF="cvRef"</code> */
  public static final String ATTR_CV_REF = "cvRef";
  /** Constant <code>ATTR_NAME="name"</code> */
  public static final String ATTR_NAME = "name";
  /** Constant <code>ATTR_ACCESSION="accession"</code> */
  public static final String ATTR_ACCESSION = "accession";
  /** Constant <code>ATTR_VALUE="value"</code> */
  public static final String ATTR_VALUE = "value";
  /** Constant <code>ATTR_UNIT_ACCESSION="unitAccession"</code> */
  public static final String ATTR_UNIT_ACCESSION = "unitAccession";
  /** Constant <code>ATTR_ENCODED_LENGTH="encodedLength"</code> */
  public static final String ATTR_ENCODED_LENGTH = "encodedLength";
  /** Constant <code>ATTR_ID_REF="idRef"</code> */
  public static final String ATTR_ID_REF = "idRef";
  /** Constant <code>ATTR_SPECTRUM_REF="spectrumRef"</code> */
  public static final String ATTR_SPECTRUM_REF = "spectrumRef";

  /**
   * Constant
   * <code>ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF="defaultInstrumentConfigurationRef"</code>
   */
  public static final String ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF =
      "defaultInstrumentConfigurationRef";
  /**
   * Constant
   * <code>ATTR_START_TIME_STAMP="startTimeStamp"</code>
   */
  public static final String ATTR_START_TIME_STAMP =
      "startTimeStamp";
  /**
   * Constant
   * <code>ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF="defaultInstrumentConfigurationRef"</code>
   */
  public static final String ATTR_DEFAULT_DATA_PROCESSING_REF = "defaultDataProcessingRef";

}
