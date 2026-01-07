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

package io.github.mzmine.util.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CsvReader {

  /**
   * Reads csv file into a list of T. enums values are case-insensitive. Unknown properties
   * (columns) are ignored
   *
   * @param file      to read
   * @param pojoClass pojo record or class
   * @param <T>       type of pojo class or record see {@link OnlineReaction} as an example
   * @return list of rows
   * @throws IOException failed to load file
   */
  public static <T> List<T> readToList(@NotNull final File file, @NotNull final Class<T> pojoClass)
      throws IOException {
    return readToList(file, pojoClass, CSVUtils.detectSeparatorFromName(file));
  }

  /**
   * Reads csv file into a list of T. enums values are case-insensitive. Unknown properties
   * (columns) are ignored
   *
   * @param file      to read
   * @param pojoClass pojo record or class
   * @param separator column separator
   * @param <T>       type of pojo class or record see {@link OnlineReaction} as an example
   * @return list of rows
   * @throws IOException failed to load file
   */
  @NotNull
  public static <T> List<T> readToList(@NotNull final File file, @NotNull final Class<T> pojoClass,
      char separator) throws IOException {
    CsvMapper mapper = CsvMapper.csvBuilder() //
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS) //
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //
        .build();
    CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(separator);
    ObjectReader reader = mapper.readerFor(pojoClass).with(schema);
    try (MappingIterator<T> iterator = reader.readValues(file)) {
      return iterator.readAll();
    }
  }

}
