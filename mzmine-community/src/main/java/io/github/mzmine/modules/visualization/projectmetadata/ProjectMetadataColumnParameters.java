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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.TextParameter;
import io.github.mzmine.util.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataColumnParameters extends SimpleParameterSet {

  private static final Logger logger = Logger.getLogger(
      ProjectMetadataColumnParameters.class.getName());
  public static final StringParameter title = new StringParameter("Title",
      "Title of the new parameter", "", true, true);

  public static final TextParameter description = new TextParameter("Description",
      "Description of the new parameter", "", false);

  /**
   * Order represents the order of value conversion in
   * {@link #castToMostAppropriateType(String[], Object[])}
   */
  public enum AvailableTypes {
    /**
     * Any number - represented as double
     */
    NUMBER(new DoubleMetadataColumn("", "")),
    /**
     * Represented as {@link LocalDateTime} see {@link DateTimeUtils}
     */
    DATETIME(new DateMetadataColumn("", "")),
    /**
     * Any string
     */
    TEXT(new StringMetadataColumn("", ""));

    private final MetadataColumn COL_INSTANCE;

    AvailableTypes(final MetadataColumn instance) {
      COL_INSTANCE = instance;
    }

    /**
     * Uses the order of values in this enum to try and convert to other data types.
     *
     * @param original  original data
     * @param converted empty array that is filled with converted values
     * @return the final type that reflects the objects of the converted array. Empty column will be
     * TEXT type
     */
    public static AvailableTypes castToMostAppropriateType(final String[] original,
        final Object[] converted) {
      for (var type : values()) {
        Object[] tmp = type.tryCastType(original);
        // error: null; all empty values: all null elements
        // empty column only accepted for text (defaults to text)
        if (tmp == null || (type != TEXT && Arrays.stream(tmp).allMatch(Objects::isNull))) {
          continue;
        }
        System.arraycopy(tmp, 0, converted, 0, tmp.length);
        return type;
      }
      throw new IllegalStateException(
          "No conversion worked. TEXT should always work though, coming from String.");
    }

    /**
     * Tries to map the values to types and returns null otherwise on error
     *
     * @param values values to map
     * @return array of same length - only if all values are mapped
     */
    public static @Nullable AvailableTypes[] tryMap(final String[] values) {
      try {
        AvailableTypes[] types = new AvailableTypes[values.length];
        for (int i = 0; i < values.length; i++) {
          types[i] = AvailableTypes.valueOf(values[i]);
        }
        return types;
      } catch (Exception e) {
        return null;
      }
    }

    public MetadataColumn getInstance() {
      return COL_INSTANCE;
    }

    /**
     * Try to cast all values to the type of the column. Empty strings and null become null.
     *
     * @param values input that should be cast
     * @return array of same length as values
     */
    public @Nullable Object[] tryCastType(final String... values) {
      if (this == TEXT) {
        return values;
      }

      String v = null;
      try {
        Object[] result = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
          v = values[i];
          result[i] = v == null || v.isBlank() ? null : getInstance().convertOrThrow(v);
        }
        return result;
      } catch (Exception ex) {
        logger.warning(
            "Cannot convert value " + Objects.requireNonNullElse(v, "null") + " to " + this);
        return null;
      }
    }
  }

  public static final ComboParameter<AvailableTypes> valueType = new ComboParameter<>("Type",
      "Type of the new parameter", AvailableTypes.values(), AvailableTypes.values()[0]);

  public ProjectMetadataColumnParameters() {
    super(new Parameter[]{title, description, valueType});
  }
}
