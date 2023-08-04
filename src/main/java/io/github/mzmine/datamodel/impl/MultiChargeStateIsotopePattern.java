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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrumType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds multiple isotope pattern of different charge states. Not every charge state is represented
 */
public class MultiChargeStateIsotopePattern implements IsotopePattern {

  public static final String XML_ELEMENT = "multi_charge_state_isotopepattern";

  public static final Comparator<IsotopePattern> patternSizeComparator = Comparator.comparingInt(
      IsotopePattern::getNumberOfDataPoints);

  @NotNull
  private final List<IsotopePattern> patterns = new ArrayList<>();

  public MultiChargeStateIsotopePattern(@NotNull IsotopePattern... patterns) {
    this(Arrays.asList(patterns));
  }

  public MultiChargeStateIsotopePattern(@NotNull List<IsotopePattern> patterns) {
    if (patterns.isEmpty()) {
      throw new IllegalArgumentException("List of isotope patterns cannot be empty");
    }
    this.patterns.addAll(patterns);
    evaluateIsotopePatterns();
  }

  public static IsotopePattern loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!reader.getLocalName().equals(XML_ELEMENT)) {
      throw new IllegalStateException("Invalid element");
    }
    List<IsotopePattern> patterns = new ArrayList<>(2);

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }

      if (SimpleIsotopePattern.XML_ELEMENT.equals(reader.getLocalName())) {
        patterns.add(SimpleIsotopePattern.loadFromXML(reader));
      }
    }
    return patterns.isEmpty() ? null : new MultiChargeStateIsotopePattern(patterns);
  }

  /**
   * List of all isotope patterns. not all charge states must be represented.
   *
   * @return isotope pattern for different charge states
   */
  public List<IsotopePattern> getPatterns() {
    return patterns;
  }

  /**
   * Add new isotope pattern to list (end of list)
   *
   * @param pattern new isotope pattern
   */
  public void addPattern(IsotopePattern pattern) {
    addPattern(pattern, false);
  }

  /**
   * Add new isotope pattern to start of list (if preferred) or end if false
   *
   * @param pattern      new isotope pattern
   * @param setPreferred true: start of list; false: end of list
   */
  public void addPattern(IsotopePattern pattern, boolean setPreferred) {
    patterns.remove(pattern);
    if (setPreferred) {
      patterns.add(0, pattern);
    } else {
      patterns.add(pattern);
      evaluateIsotopePatterns();
    }
  }

  /**
   * @param charge the charge state
   * @return returns the isotope pattern with all signals detected for this charge state
   */
  public IsotopePattern getPatternForCharge(int charge) {
    for (var pattern : patterns) {
      if (pattern.getCharge() == charge) {
        return pattern;
      }
    }
    return null;
  }

  /**
   * The preferred isotope pattern is the first in the list. usually charge 1 unless defined
   * differently
   *
   * @return the first isotope pattern
   */
  public IsotopePattern getPreferredIsotopePattern() {
    return patterns.get(0);
  }

  @Override
  public int getNumberOfDataPoints() {
    return getPreferredIsotopePattern().getNumberOfDataPoints();
  }

  @Override
  public int getCharge() {
    return getPreferredIsotopePattern().getCharge();
  }

  @Override
  public @NotNull IsotopePatternStatus getStatus() {
    return getPreferredIsotopePattern().getStatus();
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    return getPreferredIsotopePattern().getBasePeakIndex();
  }

  @Override
  public @NotNull String getDescription() {
    return getPreferredIsotopePattern().getDescription();
  }

  @Override
  public String toString() {
    return "Isotope pattern: " + getDescription();
  }

  @Override
  @NotNull
  public Range<Double> getDataPointMZRange() {
    return getPreferredIsotopePattern().getDataPointMZRange();
  }

  @Override
  public @NotNull Double getTIC() {
    return getPreferredIsotopePattern().getTIC();
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return MassSpectrumType.CENTROIDED;
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    return getPreferredIsotopePattern().getMzValues(dst);
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    return getPreferredIsotopePattern().getIntensityValues(dst);
  }

  @Override
  public double getMzValue(int index) {
    return getPreferredIsotopePattern().getMzValue(index);
  }

  @Override
  public double getIntensityValue(int index) {
    return getPreferredIsotopePattern().getIntensityValue(index);
  }

  @Override
  @Nullable
  public Double getBasePeakMz() {
    return getPreferredIsotopePattern().getBasePeakMz();
  }

  @Override
  @Nullable
  public Double getBasePeakIntensity() {
    return getPreferredIsotopePattern().getBasePeakIntensity();
  }

  @Override
  public Iterator<DataPoint> iterator() {
    return getPreferredIsotopePattern().iterator();
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    for (IsotopePattern pattern : patterns) {
      pattern.saveToXML(writer);
    }

    writer.writeEndElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MultiChargeStateIsotopePattern that = (MultiChargeStateIsotopePattern) o;
    return patterns.equals(that.patterns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(patterns);
  }

  /**
   * Sorts the isotope patterns by pattern size.
   */
  private void evaluateIsotopePatterns() {
    patterns.sort(patternSizeComparator);
  }
}
