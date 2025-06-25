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

package io.github.mzmine.util.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

  /**
   * thread safe single instance. Better to reuse as it is costly and caches etc
   */
  public static final ObjectMapper MAPPER = new ObjectMapper();


  /**
   * Write json string or throw {@link RuntimeException}
   */
  public static String writeStringOrThrow(final Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write json string or empty string on exception
   */
  public static String writeStringOrEmpty(final Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "";
    }
  }

  /**
   * Write value or return default on exception
   */
  public static String writeStringOrElse(final Object value, final String defaultValue) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return defaultValue;
    }
  }

  public static <T> T readValueOrThrow(final String content) {
    try {
      return MAPPER.readValue(content, new TypeReference<T>() {
      });
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T readValueOrElse(final String content, final T defaultValue) {
    try {
      return MAPPER.readValue(content, new TypeReference<T>() {
      });
    } catch (JsonProcessingException e) {
      return defaultValue;
    }
  }

  public static <T> T readValueOrNull(final String content) {
    try {
      return MAPPER.readValue(content, new TypeReference<T>() {
      });
    } catch (JsonProcessingException e) {
      return null;
    }
  }
}
