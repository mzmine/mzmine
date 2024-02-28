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

package import_data;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class DataFileStatsIO {

  private static final Logger logger = Logger.getLogger(DataFileStatsIO.class.getName());

  public static void writeJson(String path, Class<?> clazz, List<DataFileStats> stats) {
    var file = getFile(path, clazz);
    FileAndPathUtil.createDirectory(file.getParentFile());
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      objectMapper.writeValue(file, stats);
      logger.info("Test file written to " + file.getAbsolutePath());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot write " + e.getMessage(), e);
    }
  }

  @NotNull
  private static File getFile(String path, final Class<?> clazz) {
    return Path.of(path, getResource(clazz)).toFile();
  }

  private static String getResource(final Class<?> clazz) {
    return "rawdatafiles/testdata/test_%s.json".formatted(clazz.getName());
  }

  public static Map<String, DataFileStats> readJson(Class<?> clazz) {
    String fileStr = requireNonNull(
        clazz.getClassLoader().getResource(getResource(clazz))).getFile();
    File file = new File(fileStr);
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      DataFileStats[] stats = objectMapper.readValue(file, DataFileStats[].class);
      return Arrays.stream(stats)
          .collect(Collectors.toMap(s -> s.fileName() + s.advanced(), s -> s));
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot write " + e.getMessage(), e);
      throw new RuntimeException("Cannot write " + e.getMessage(), e);
    }
  }
}
