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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identity of an other-detector trace, used to decide which {@link OtherFeature}s may be aligned
 * across raw data files. This is the "m/z equivalent" for other detectors: alignment only ever
 * compares features that share an equal {@link TraceKey}, while retention time remains the second
 * axis.
 * <p>
 * The key is composed of the {@link ChromatogramType}, an optional wavelength (UV/DAD traces), the
 * trace name (usually derived from {@link OtherTimeSeries#getName()}), and the owning
 * {@link OtherDataFile}'s description. Matching is exact on all components (record equality); any of
 * {@code wavelength}/{@code name}/{@code otherFileDescription} may be {@code null} (e.g. CAD has no
 * wavelength).
 * <p>
 * The {@code otherFileDescription} discriminator lets a single {@link RawDataFile} that holds more
 * than one {@link OtherDataFile} keep those detectors apart even when they would otherwise share a
 * key (e.g. two PDA detectors both producing a "250 nm" absorption trace). Because the description
 * is stable for a given acquisition method, cross-file alignment of the same detector still works.
 * This is a canonical key: any relaxation (e.g. CAD forcing a null wavelength) must be applied while
 * building the key in {@link #of(OtherFeature)}, never via a separate matcher that would declare
 * unequal keys equivalent.
 */
public record TraceKey(@NotNull ChromatogramType chromatogramType, @Nullable Double wavelength,
                       @Nullable String name, @Nullable String otherFileDescription) {

  public static final String XML_ELEMENT = "tracekey";
  private static final String XML_CHROMATOGRAM_TYPE_ATTR = "chromatogramtype";
  private static final String XML_WAVELENGTH_ATTR = "wavelength";
  private static final String XML_NAME_ATTR = "name";
  private static final String XML_OTHER_FILE_DESCRIPTION_ATTR = "otherfiledescription";

  /**
   * Builds the {@link TraceKey} describing the given feature. Reads the chromatogram type, wavelength,
   * the underlying time series name, and the owning other-data-file description.
   */
  @NotNull
  public static TraceKey of(@NotNull final OtherFeature feature) {
    final ChromatogramType chromType = feature.getChromatogramType();
    final OtherTimeSeries data = feature.getFeatureData();
    // assumption: same acquisition method across a batch yields identical time series names and
    // other-data-file descriptions
    final String name = data != null ? data.getName() : null;
    final String otherFileDescription =
        data != null ? data.getOtherDataFile().getDescription() : null;
    return new TraceKey(chromType != null ? chromType : ChromatogramType.UNKNOWN,
        feature.getWavelength(), name, otherFileDescription);
  }

  public void saveToXML(@NotNull final XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_CHROMATOGRAM_TYPE_ATTR, chromatogramType.getUniqueID());
    if (wavelength != null) {
      writer.writeAttribute(XML_WAVELENGTH_ATTR, String.valueOf(wavelength));
    }
    if (name != null) {
      writer.writeAttribute(XML_NAME_ATTR, name);
    }
    if (otherFileDescription != null) {
      writer.writeAttribute(XML_OTHER_FILE_DESCRIPTION_ATTR, otherFileDescription);
    }
    writer.writeEndElement();
  }

  @NotNull
  public static TraceKey loadFromXML(@NotNull final XMLStreamReader reader) {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Wrong element for TraceKey: " + reader.getLocalName());
    }

    final ChromatogramType chromType = UniqueIdSupplier.parseOrElse(
        reader.getAttributeValue(null, XML_CHROMATOGRAM_TYPE_ATTR), ChromatogramType.values(),
        ChromatogramType.UNKNOWN);

    final String wavelengthStr = reader.getAttributeValue(null, XML_WAVELENGTH_ATTR);
    Double wavelength = null;
    if (wavelengthStr != null) {
      try {
        wavelength = Double.parseDouble(wavelengthStr);
      } catch (NumberFormatException e) {
        // ignore malformed wavelength
      }
    }

    final String name = reader.getAttributeValue(null, XML_NAME_ATTR);
    final String otherFileDescription = reader.getAttributeValue(null,
        XML_OTHER_FILE_DESCRIPTION_ATTR);
    return new TraceKey(chromType, wavelength, name, otherFileDescription);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (name != null && !name.isBlank()) {
      sb.append(name);
    } else {
      sb.append(chromatogramType.toString());
    }
    if (wavelength != null) {
      sb.append(" (").append(wavelength).append(" nm)");
    }
    return sb.toString();
  }
}
