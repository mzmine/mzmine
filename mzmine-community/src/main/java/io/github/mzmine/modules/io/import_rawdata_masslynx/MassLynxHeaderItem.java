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

package io.github.mzmine.modules.io.import_rawdata_masslynx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.CalibrationDateTimeType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.LcMethodNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.OperatorNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleDescriptionType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.TuneMethodNameType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.AcquisitionMethodType;
import org.jetbrains.annotations.Nullable;

/**
 * Header item identifiers used by MassLynx, starting at 300. Items with a
 * {@link #getFileMetadataType() file metadata type} are read during import and stored in
 * {@link RawDataFile#getFileMetadata()}, see {@link MassLynxDataAccess#applyToFileMetadata}.
 */
public enum MassLynxHeaderItem {
  // MassLynx version, the acquisition software is set to MassLynx
  VERSION(300, AcquisitionSoftwareVersionType.class), //
  ACQUIRED_NAME(301), ACQUIRED_DATE(302), ACQUIRED_TIME(303), JOB_CODE(304), TASK_CODE(305), //
  USER_NAME(306, OperatorNameType.class), //
  INSTRUMENT(307, InstrumentModelType.class), //
  CONDITIONS(308), LAB_NAME(309), //
  SAMPLE_DESCRIPTION(310, SampleDescriptionType.class), //
  SOLVENT_DELAY(311), SUBMITTER(312), //
  SAMPLE_ID(313, SampleNameType.class), //
  BOTTLE_NUMBER(314), ANALOG_CH1_OFFSET(315), ANALOG_CH2_OFFSET(316), ANALOG_CH3_OFFSET(317), //
  ANALOG_CH4_OFFSET(318), CAL_MS1_STATIC(319), CAL_MS2_STATIC(320), CAL_MS1_STATIC_PARAMS(321), //
  CAL_MS1_DYNAMIC_PARAMS(322), CAL_MS2_STATIC_PARAMS(323), CAL_MS2_DYNAMIC_PARAMS(324), //
  CAL_MS1_FAST_PARAMS(325), CAL_MS2_FAST_PARAMS(326), //
  // calibration date and time are combined into one value
  CAL_TIME(327, CalibrationDateTimeType.class), //
  CAL_DATE(328, CalibrationDateTimeType.class), //
  CAL_TEMPERATURE(329), //
  // LC method if HPLC_METHOD is not set
  INLET_METHOD(330, LcMethodNameType.class), //
  SPARE1(331), SPARE2(332), SPARE3(333), SPARE4(334), //
  SPARE5(335), //
  MS_METHOD(336, AcquisitionMethodType.class), //
  INLET_PRERUN_METHOD(337), INLET_POSTRUN_METHOD(338), INLET_SWITCH_METHOD(339), //
  HPLC_METHOD(340, LcMethodNameType.class), //
  TUNE_METHOD(341, TuneMethodNameType.class), //
  FRACTIONLYNX_METHOD(342), REINJECTIONS(343), PIC_MRM_FUNCTION(344), PIC_SCAN_FUNCTION(345), //
  SCANWAVE_FUNC_LIST(346), CALIBRATION_FILE(347), ASSOCIATED_DATAFILE(348), MUX_STREAM(349);

  private final int value;
  private final @Nullable Class<? extends DataType<?>> fileMetadataType;

  MassLynxHeaderItem(final int value) {
    this(value, null);
  }

  MassLynxHeaderItem(final int value,
      @Nullable final Class<? extends DataType<?>> fileMetadataType) {
    this.value = value;
    this.fileMetadataType = fileMetadataType;
  }

  public int getValue() {
    return value;
  }

  /**
   * @return the raw file metadata type this item is mapped to or null
   */
  public @Nullable Class<? extends DataType<?>> getFileMetadataType() {
    return fileMetadataType;
  }

  /**
   * @return true if this item is read during import and mapped to the raw file metadata
   */
  public boolean hasFileMetadataType() {
    return fileMetadataType != null;
  }
}