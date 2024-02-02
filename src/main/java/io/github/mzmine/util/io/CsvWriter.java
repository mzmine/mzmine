/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.jetbrains.annotations.NotNull;

public class CsvWriter {

  public static <T> void writeToFile(@NotNull File outFile, Iterable<T> items, Class<T> pojoClass,
      boolean append) throws IOException {
    FileAndPathUtil.createDirectory(outFile.getParentFile());
    boolean createHeader = !(append && outFile.exists());
    CsvMapper tsvMapper = new CsvMapper();
    CsvSchema schema = tsvMapper.schemaFor(pojoClass).withUseHeader(createHeader);

    try (var fileWriter = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8,
        StandardOpenOption.APPEND, StandardOpenOption.CREATE); //
        SequenceWriter sequenceWriter = tsvMapper.writer(schema).writeValues(fileWriter)) {

      for (final T item : items) {
        sequenceWriter.write(item);
      }
    }
  }
}
