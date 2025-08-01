/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coollects traces from a single detector. these traces may be read from the raw data or created
 * from {@link OtherSpectralData} by slicing along the retention time axis.
 */
public interface OtherTimeSeriesData {

  String XML_ELEMENT = "othertimeseriesdata";

  OtherDataFile getOtherDataFile();

  @NotNull String getTimeSeriesDomainLabel();

  @NotNull String getTimeSeriesDomainUnit();

  @NotNull String getTimeSeriesRangeLabel();

  @NotNull String getTimeSeriesRangeUnit();

  /**
   * @return The actual raw data, without any preprocessing applied
   */
  @NotNull List<@NotNull OtherFeature> getRawTraces();

  /**
   * @return The raw traces with applied preprocessing, such as rt shifting or baseline correction.
   * no feature detection has been applied. If no preprocessing was applied, the raw traces are
   * returned.
   */
  @NotNull List<@NotNull OtherFeature> getPreprocessedTraces();

  /**
   * Replaces all existing preprocessed traces.
   *
   * @param preprocessedTraces The preprocessed traces.
   */
  void setPreprocessedTraces(@NotNull List<@NotNull OtherFeature> preprocessedTraces);

  default int getNumberOfTimeSeries() {
    return getRawTraces().size();
  }

  @NotNull OtherFeature getRawTrace(int index);

  /**
   * @return The chromatograms in this data file or null if this file does not contain
   * chromatograms.
   */
  @NotNull ChromatogramType getChromatogramType();

  List<OtherFeature> getProcessedFeatures();

  /**
   * @return The processed features for the given series, may be empty. The list is modifiable.
   */
  @NotNull List<OtherFeature> getProcessedFeaturesForTrace(OtherFeature rawTrace);

  /**
   * @return The preprocessed traces for the given series, may be empty. The list is modifiable.
   */
  @Nullable OtherFeature getPreProcessedFeatureForTrace(@Nullable OtherFeature rawTrace);

  void replaceProcessedFeaturesForTrace(OtherFeature rawTrace,
      @NotNull List<OtherFeature> newFeatures);

  void addProcessedFeature(@NotNull OtherFeature newFeature);

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  static OtherTimeSeriesData loadFromXML(XMLStreamReader reader, RawDataFile file)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Wrong element");
    }

    final String rawFileName = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
    final String otherDataFileDesc = reader.getElementText();
    if (!Objects.equals(rawFileName, file.getFileName())) {
      throw new IllegalStateException("Raw files don't match.");
    }
    return file.getOtherDataFiles().stream()
        .filter(odf -> odf.getDescription().equals(otherDataFileDesc)).findFirst().map(OtherDataFile::getOtherTimeSeriesData)
        .orElse(null);
  }
}
