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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Holds a normalization function that should be applied to intensity values of features.
 *
 * @param rawDataFilePlaceholder points to the raw data file
 * @param acquisitionTimestamp   the acquisition time of the raw data file for interpolation or null
 *                               if unknown
 * @param function               the function
 */
public record RawFileNormalizationFunction(@NotNull RawDataFilePlaceholder rawDataFilePlaceholder,
                                           @Nullable LocalDateTime acquisitionTimestamp,
                                           @NotNull NormalizationFunction function) {


  public static final String XML_ACQUISITION_TIMESTAMP_ATTR = "acquisitionTimestamp";
  public static final String XML_PARENT_ELEMENT = "rawFileNormalization";

  public RawFileNormalizationFunction (@NotNull RawDataFile file, @NotNull NormalizationFunction function) {
    final LocalDateTime runDate = MetadataTableUtils.getRunDate(file);
    this(new RawDataFilePlaceholder(file), runDate, function);
  }

  /**
   *
   * @return The data file this normalization applies to. may be null if the file was removed from
   * the project.
   */
  public @Nullable RawDataFile getRawDataFile() {
    return rawDataFilePlaceholder().getMatchingFile();
  }

  /**
   * @return Create a copy of the function that points to the new file
   */
  public @NotNull RawFileNormalizationFunction withRawFile(@NotNull RawDataFile file) {
    final LocalDateTime runDate = MetadataTableUtils.getRunDate(file);
    return new RawFileNormalizationFunction(new RawDataFilePlaceholder(file), runDate, function);
  }


  public void saveToXML(@NotNull Element element) {
    rawDataFilePlaceholder.saveToXML(element);
    if (acquisitionTimestamp != null) {
      element.setAttribute(XML_ACQUISITION_TIMESTAMP_ATTR, acquisitionTimestamp.toString());
    }

    function.saveToXML(element);
  }


  public void appendFunctionElement(final @NotNull Element parentElement) {
    final Document document = parentElement.getOwnerDocument();
    final Element functionElement = document.createElement(XML_PARENT_ELEMENT);
    saveToXML(functionElement);
    parentElement.appendChild(functionElement);
  }

  static @NotNull RawFileNormalizationFunction loadFromXML(final @NotNull Element parentElement) {

    final RawDataFilePlaceholder rawFile = RawDataFilePlaceholder.loadFromXML(parentElement);
    final LocalDateTime acqTime = loadAcquisitionTimestamp(parentElement);

    final NormalizationFunction func = NormalizationFunction.loadFromXML(parentElement);

    return new RawFileNormalizationFunction(rawFile, acqTime, func);
  }

  static @Nullable LocalDateTime loadAcquisitionTimestamp(final @NotNull Element functionElement) {
    if (!functionElement.hasAttribute(XML_ACQUISITION_TIMESTAMP_ATTR)) {
      return null;
    }

    final String acquisitionTimestamp = functionElement.getAttribute(
        XML_ACQUISITION_TIMESTAMP_ATTR);
    if (acquisitionTimestamp.isBlank()) {
      return null;
    }

    return LocalDateTime.parse(acquisitionTimestamp);
  }

}
