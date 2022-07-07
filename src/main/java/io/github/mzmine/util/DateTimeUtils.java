/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.jetbrains.annotations.NotNull;

/**
 * Parse and format dates. MZmine uses {@link LocalDateTime} to represent date + time in this
 * format: 2022-06-01T18:36:09
 * <p>
 * 2022-06-01T18:36:09Z is a zoned format that needs to be parsed by {@link ZonedDateTime}
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class DateTimeUtils {

  /**
   * Obtains an instance of LocalDateTime from a text string such as 2007-12-03T10:15:30. The string
   * must represent a valid date-time and is parsed using DateTimeFormatter.ISO_LOCAL_DATE_TIME.
   *
   * @param dateTime the text to parse such as "2007-12-03T10:15:30" or "2007-12-03T10:15:30Z"
   * @return the parsed local date-time, not null
   * @throws java.time.format.DateTimeParseException – if the text cannot be parsed
   */
  @NotNull
  public static LocalDateTime parse(@NotNull String dateTime) {
    try {
      // ZonedDateTime with 2022-06-01T18:36:09Z where the Z stands for UTC
      return ZonedDateTime.parse(dateTime).toLocalDateTime();
    } catch (Exception ignored) {
      // try to parse LocalDateTime 2022-06-01T18:36:09
      return LocalDateTime.parse(dateTime);
    }
  }

}
