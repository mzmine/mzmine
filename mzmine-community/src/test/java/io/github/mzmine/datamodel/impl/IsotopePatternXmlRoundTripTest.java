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

package io.github.mzmine.datamodel.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * XML persistence of isotope patterns: the detection score must survive a project save/load, and a
 * multi-charge pattern must reload with the same preferred charge it was written with.
 */
class IsotopePatternXmlRoundTripTest {

  private static final double C13 = 1.0033548;

  @Test
  void scoreSurvivesRoundTrip() throws XMLStreamException {
    final SimpleIsotopePattern pattern = pattern(500d, 1, 0.7345d);

    final IsotopePattern loaded = roundTrip(pattern, SimpleIsotopePattern.XML_ELEMENT);

    assertNotNull(loaded);
    assertEquals(0.7345d, loaded.getScore(), 1e-9, "the stored score must be persisted");
    assertEquals(1, loaded.getCharge());
    assertEquals(pattern.getNumberOfDataPoints(), loaded.getNumberOfDataPoints());
    assertEquals(pattern.getMzValue(0), loaded.getMzValue(0), 1e-9);
  }

  @Test
  void unscoredPatternReloadsAsUnscored() throws XMLStreamException {
    // a predicted (not detected) pattern carries no score; the element is omitted and must not be
    // read back as 0, which would rank it below every scored pattern instead of "unknown"
    final SimpleIsotopePattern pattern = new SimpleIsotopePattern(
        new DataPoint[]{new SimpleDataPoint(500d, 100d)}, 1, IsotopePatternStatus.PREDICTED, "test");

    final IsotopePattern loaded = roundTrip(pattern, SimpleIsotopePattern.XML_ELEMENT);

    assertNotNull(loaded);
    assertTrue(Double.isNaN(loaded.getScore()), "an unscored pattern must stay unscored");
  }

  @Test
  void multiChargePatternKeepsItsPreferredChargeAcrossRoundTrip() throws XMLStreamException {
    // the engine ranks by its own selection score, which is NOT the stored pattern score, so the
    // written order can disagree with a score sort. Reloading must not re-rank.
    final IsotopePattern winner = pattern(500d, 2, 0.4d);
    final IsotopePattern alternate = pattern(500d, 1, 0.9d);
    final MultiChargeStateIsotopePattern multi = MultiChargeStateIsotopePattern.ofRanked(
        List.of(winner, alternate));
    assertEquals(2, multi.getCharge(), "the ranked order defines the preferred charge");

    final IsotopePattern loaded = roundTrip(multi, MultiChargeStateIsotopePattern.XML_ELEMENT);

    assertNotNull(loaded);
    assertEquals(2, loaded.getCharge(),
        "a reload must not promote the higher-scoring alternate to preferred");
  }

  private static @NotNull SimpleIsotopePattern pattern(final double mz, final int charge,
      final double score) {
    return new SimpleIsotopePattern(
        new DataPoint[]{new SimpleDataPoint(mz, 100d), new SimpleDataPoint(mz + C13 / charge, 30d)},
        charge, score, IsotopePatternStatus.DETECTED, "test z=" + charge);
  }

  /**
   * Write the pattern to XML and read it back, positioned on the given root element.
   */
  private static IsotopePattern roundTrip(@NotNull final IsotopePattern pattern,
      @NotNull final String rootElement) throws XMLStreamException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
    writer.writeStartDocument();
    pattern.saveToXML(writer);
    writer.writeEndDocument();
    writer.close();

    final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(
        new ByteArrayInputStream(out.toByteArray()));
    while (reader.hasNext()) {
      if (reader.next() == XMLEvent.START_ELEMENT && reader.getLocalName().equals(rootElement)) {
        return MultiChargeStateIsotopePattern.XML_ELEMENT.equals(rootElement)
            ? MultiChargeStateIsotopePattern.loadFromXML(reader)
            : SimpleIsotopePattern.loadFromXML(reader);
      }
    }
    throw new IllegalStateException("root element " + rootElement + " not written");
  }
}
