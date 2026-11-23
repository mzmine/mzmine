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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentSerialNumberType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentVendorType;
import io.github.mzmine.datamodel.features.rawfiletypes.IonSourcesType;
import io.github.mzmine.datamodel.features.rawfiletypes.MassAnalyzersType;
import io.github.mzmine.datamodel.features.rawfiletypes.OperatorNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleDescriptionType;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleNameType;
import io.github.mzmine.datamodel.features.types.abstr.StringListType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.AcquisitionMethodType;
import io.github.mzmine.util.date.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * File level metadata of an Agilent .d file (sample, operator, instrument, acquisition),
 * independent of the reader that provides it. Every value is optional.
 *
 * @param acquisitionDateTime ISO 8601: recent files store UTC ("2014-11-18T03:54:47Z"), old files
 *                            an unzoned local time ("2006-06-14T16:26:28")
 * @param ionSources          PSI-MS names
 * @param massAnalyzers       PSI-MS names
 */
public record AgilentFileMetadata(@Nullable String sampleName, @Nullable String sampleId,
                                  @Nullable String sampleDescription, @Nullable String operatorName,
                                  @Nullable String instrumentModel,
                                  @Nullable String instrumentSerialNumber,
                                  @Nullable String instrumentVendor,
                                  @Nullable String acquisitionSoftware,
                                  @Nullable String acquisitionSoftwareVersion,
                                  @Nullable String acquisitionDateTime,
                                  @Nullable String acquisitionMethod,
                                  @NotNull List<String> ionSources,
                                  @NotNull List<String> massAnalyzers) {

  private static final Logger logger = Logger.getLogger(AgilentFileMetadata.class.getName());

  /**
   * @return the acquisition date and time, null if not available. UTC times are converted by
   * {@link DateTimeUtils#parse(String)}, unzoned times are kept.
   */
  public @Nullable LocalDateTime acquisitionDate(@NotNull String fileName) {
    if (acquisitionDateTime == null) {
      return null;
    }
    try {
      return DateTimeUtils.parse(acquisitionDateTime);
    } catch (RuntimeException e) {
      logger.fine(
          "Cannot parse acquisition date '%s' of file %s".formatted(acquisitionDateTime, fileName));
      return null;
    }
  }

  /**
   * Sets the values to the raw file metadata. Mirrors
   * {@code MassLynxDataAccess#applyToFileMetadata}.
   */
  public void applyTo(@NotNull final ModularDataModel metadata) {
    // decision: the sample ID is the sample name if no name was entered
    setText(metadata, SampleNameType.class, sampleName != null ? sampleName : sampleId);
    setText(metadata, SampleDescriptionType.class, sampleDescription);
    setText(metadata, OperatorNameType.class, operatorName);
    setText(metadata, InstrumentModelType.class, instrumentModel);
    setText(metadata, InstrumentSerialNumberType.class, instrumentSerialNumber);
    setText(metadata, InstrumentVendorType.class, instrumentVendor);
    setText(metadata, AcquisitionSoftwareType.class, acquisitionSoftware);
    setText(metadata, AcquisitionSoftwareVersionType.class, acquisitionSoftwareVersion);
    setText(metadata, AcquisitionMethodType.class, acquisitionMethod);
    setList(metadata, IonSourcesType.class, ionSources);
    setList(metadata, MassAnalyzersType.class, massAnalyzers);
  }

  private static void setText(@NotNull ModularDataModel metadata,
      @NotNull Class<? extends StringType> type, @Nullable String value) {
    if (value != null && !value.isBlank()) {
      metadata.set(type, value.strip());
    }
  }

  private static void setList(@NotNull ModularDataModel metadata,
      @NotNull Class<? extends StringListType> type, @NotNull List<String> values) {
    final List<String> clean = values.stream().filter(v -> v != null && !v.isBlank())
        .map(String::strip).toList();
    if (!clean.isEmpty()) {
      metadata.set(type, List.copyOf(clean));
    }
  }
}
