/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility functions used during project load/save.
 */
public class ParsingUtils {

  private static final Logger logger = Logger.getLogger(ParsingUtils.class.getName());

  /**
   * Value separator for storing lists and arrays.
   */
  public static String SEPARATOR = ";";

  public static double[] stringToDoubleArray(String string) {
    final String[] strValues = string.split(ParsingUtils.SEPARATOR);
    final double[] values = new double[strValues.length];
    for (int i = 0; i < strValues.length; i++) {
      values[i] = Double.parseDouble(strValues[i]);
    }
    return values;
  }

  public static String doubleArrayToString(double[] array) {
    return doubleArrayToString(array, array.length);
  }

  public static String doubleArrayToString(double[] array, int length) {
    assert length <= array.length;

    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      double v = array[i];
      b.append(v);
      if (i < length - 1) {
        b.append(SEPARATOR);
      }
    }
    return b.toString();
  }

  public static String doubleBufferToString(DoubleBuffer buffer) {
    StringBuilder b = new StringBuilder();
    for (int i = 0, arrayLength = buffer.capacity(); i < arrayLength; i++) {
      double v = buffer.get(i);
      b.append(v);
      if (i < arrayLength - 1) {
        b.append(SEPARATOR);
      }
    }
    return b.toString();
  }

  public static String intArrayToString(int[] array, int length) {
    assert length <= array.length;

    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      int v = array[i];
      b.append(v);
      if (i < length - 1) {
        b.append(SEPARATOR);
      }
    }
    return b.toString().trim();
  }

  public static int[] stringToIntArray(String string) {
    final String[] strValues = string.split(ParsingUtils.SEPARATOR);
    final int[] values = new int[strValues.length];
    for (int i = 0; i < strValues.length; i++) {
      values[i] = Integer.parseInt(strValues[i]);
    }
    return values;
  }

  public static <T extends Object> int[] getIndicesOfSubListElements(List<T> sublist,
      List<T> fullList) {
    final int[] indices = new int[sublist.size()];

    int rawIndex = fullList.indexOf(sublist.get(0));
    int subListIndex = 0;
    indices[subListIndex] = rawIndex;

    while (subListIndex < sublist.size() && rawIndex < fullList.size()) {
      if (sublist.get(subListIndex)
          .equals(fullList.get(rawIndex))) { // don't compare identity to make robin happy
        indices[subListIndex] = rawIndex;
        subListIndex++;
      }
      rawIndex++;
    }

    if (subListIndex < sublist.size()) {
      throw new IllegalStateException(
          "Incomplete remap. Sublist did not contain all scans in the fullList.");
    }

    return indices;
  }

  public static <T extends Object> List<T> getSublistFromIndices(List<T> list, int[] indices) {
    List<T> sublist = new ArrayList<>();
    for (int index : indices) {
      sublist.add(list.get(index));
    }
    return sublist;
  }


  public static String rangeToString(Range<Comparable<?>> range) {
    return "[" + range.lowerEndpoint() + SEPARATOR + range.upperEndpoint() + "]";
  }

  public static Range<Double> stringToDoubleRange(String str) {
    if (str.isEmpty()) {
      return null;
    }
    String[] vals = str.replaceAll("\\[", "").replaceAll("\\]", "").split(SEPARATOR);
    if (vals.length != 2) {
      throw new IllegalStateException("Error while parsing double range from string " + str);
    }
    return Range.closed(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]));
  }

  public static Range<Float> stringToFloatRange(String str) {
    if (str.isEmpty()) {
      return null;
    }
    String[] vals = str.replaceAll("\\[", "").replaceAll("\\]", "").split(SEPARATOR);
    if (vals.length != 2) {
      throw new IllegalStateException("Error while parsing float range from string " + str);
    }
    return Range.closed(Float.parseFloat(vals[0]), Float.parseFloat(vals[1]));
  }

  public static Range<Integer> parseIntegerRange(String str) {
    Pattern regex = Pattern.compile("\\[([0-9]+)" + SEPARATOR + "([0-9]+)\\]");
    Matcher matcher = regex.matcher(str);
    if (matcher.matches()) {
      int lower = Integer.parseInt(matcher.group(1));
      int upper = Integer.parseInt(matcher.group(2));
      return Range.closed(lower, upper);
    }
    return null;
  }

  public static String mobilityScanListToString(List<MobilityScan> scans) {
    // {frameindex}[mobilityscanindices]\\
    StringBuilder b = new StringBuilder();
    final Map<Frame, List<MobilityScan>> mapping = scans.stream()
        .collect(Collectors.groupingBy(MobilityScan::getFrame));
    for (Iterator<Entry<Frame, List<MobilityScan>>> it = mapping.entrySet().iterator();
        it.hasNext(); ) {
      Entry<Frame, List<MobilityScan>> entry = it.next();
      Frame frame = entry.getKey();
      List<MobilityScan> mobilityScans = entry.getValue();
      mobilityScans.sort(Comparator.comparingInt(MobilityScan::getMobilityScanNumber));
      b.append("{").append(frame.getDataFile().getScans().indexOf(frame)).append("}");

      int[] indices = ParsingUtils.getIndicesOfSubListElements(mobilityScans,
          frame.getMobilityScans());
      b.append("[").append(ParsingUtils.intArrayToString(indices, indices.length)).append("]");

      if (it.hasNext()) {
        b.append(SEPARATOR).append(SEPARATOR);
      }
    }
    return b.toString();
  }

  @Nullable
  public static List<MobilityScan> stringToMobilityScanList(String str, IMSRawDataFile file) {
    final List<MobilityScan> mobilityScans = new ArrayList<>();
    final String[] split = str.split(SEPARATOR + SEPARATOR);
    final Pattern pattern = Pattern.compile("\\{([0-9]+)\\}\\[([^\\n]+)\\]");

    for (final String s : split) {
      Matcher matcher = pattern.matcher(s);
      if (matcher.matches()) {
        int frameIndex = Integer.parseInt(matcher.group(1));
        Frame frame = file.getFrame(frameIndex);

        int[] indices = stringToIntArray(matcher.group(2));
        mobilityScans.addAll(ParsingUtils.getSublistFromIndices(frame.getMobilityScans(), indices));
      } else {
        throw new IllegalStateException("Pattern does not match");
      }
    }

    return mobilityScans.isEmpty() ? null : mobilityScans;
  }

  public static String stringArrayToString(String[] array) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < array.length; i++) {
      b.append(array[i]);
      if (i < array.length - 1) {
        b.append(SEPARATOR);
      }
    }
    return b.toString();
  }

  public static String[] stringToStringArray(String str) {
    return str.split(SEPARATOR);
  }

  public static IonizationType ionizationNameToIonizationType(String ionizationName) {
    IonizationType[] ionizationTypes = IonizationType.class.getEnumConstants();
    for (IonizationType ionizationType : ionizationTypes) {
      if (ionizationType.name().equals(ionizationName)) {
        return ionizationType;
      }
    }
    return null;
  }

  public static PolarityType polarityNameToPolarityType(String polarityName) {
    PolarityType[] polarityTypes = PolarityType.class.getEnumConstants();
    for (PolarityType polarityType : polarityTypes) {
      if (polarityType.name().equals(polarityName)) {
        return polarityType;
      }
    }
    return null;
  }

  /**
   * @param reader           The reader.
   * @param attributeName    The name of the attribute to parse.
   * @param defaultValue     A default vaule, if the attribute is not found or cannot be parsed with
   *                         the given function.
   * @param conversionMethod A function to convert the parsed string to a value of type T.
   * @param <T>              The type of the value to parse.
   * @return The parsed value or the default value. If the attribute does not exist or an error
   * occurs during parsing, the
   */
  public static <T> T readAttributeValueOrDefault(@NotNull final XMLStreamReader reader,
      String attributeName, T defaultValue, Function<String, T> conversionMethod) {

    if (!reader.isStartElement()) {
      throw new IllegalStateException(
          "Error parsing attribute. Current element is not a start element.");
    }

    final String attributeValue = reader.getAttributeValue(null, attributeName);
    if (attributeValue == null) {
      return defaultValue;
    }

    try {
      return conversionMethod.apply(attributeValue);
    } catch (Exception e) {
      logger.log(Level.WARNING,
          "Error parsing attribute " + attributeName + "of element " + reader.getLocalName()
              + " with value " + attributeValue);
      return defaultValue;
    }
  }

  /**
   * @param str
   * @return The string representation for saving strings to xml values. "null" is replaced by
   * {@link CONST#XML_NULL_VALUE}.
   * @see ParsingUtils#readNullableString
   */
  @NotNull
  public static String parseNullableString(@Nullable String str) {
    return str != null ? str : CONST.XML_NULL_VALUE;
  }

  /**
   * @param str
   * @return The value for the given string representation. {@link CONST#XML_NULL_VALUE} is replaced
   * by null.
   * @see ParsingUtils#parseNullableString(String)
   */
  @Nullable
  public static String readNullableString(@NotNull String str) {
    return str.trim().equals(CONST.XML_NULL_VALUE) ? null : str;
  }

  public static boolean progressToStartElement(@NotNull XMLStreamReader reader,
      @NotNull final String startElement, @NotNull final String breakpointEndElement)
      throws XMLStreamException {
    while (reader.hasNext() && !(reader.isStartElement() && reader.getLocalName()
        .equals(startElement))) {
      if (reader.isEndElement() && reader.getLocalName().equals(breakpointEndElement)) {
        return false;
      }
      reader.next();
    }
    return true;
  }

  public static IonType parseIon(String str) {
    Pattern.compile("(\\[)?(\\d*)(M)([\\+\\-])([a-zA-Z_0-9\\\\+\\\\-]*)([\\]])?([\\d])?([\\+\\-])");
    return null;
  }

  /**
   * @param number A number or null
   * @return The string representation of the given number. ({@link CONST#XML_NULL_VALUE} for null).
   */
  @NotNull
  public static String numberToString(@Nullable Number number) {
    if (number == null) {
      return CONST.XML_NULL_VALUE;
    } else {
      return String.valueOf(number);
    }
  }

  /**
   * Converts a string to a double. If the string is equal to {@link CONST#XML_NULL_VALUE}, null is
   * returned.
   *
   * @param str The string.
   * @return The Double.
   */
  @Nullable
  public static Double stringToDouble(@Nullable String str) {
    if (str == null || str.equals(CONST.XML_NULL_VALUE)) {
      return null;
    }

    try {
      return Double.valueOf(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Converts a string to a float. If the string is equal to {@link CONST#XML_NULL_VALUE}, null is
   * returned.
   *
   * @param str The string.
   * @return The float.
   */
  @Nullable
  public static Float stringToFloat(@Nullable String str) {
    if (str == null || str.equals(CONST.XML_NULL_VALUE)) {
      return null;
    }

    try {
      return Float.valueOf(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
