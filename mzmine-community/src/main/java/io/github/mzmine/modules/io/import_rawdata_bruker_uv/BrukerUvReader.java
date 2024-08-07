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

package io.github.mzmine.modules.io.import_rawdata_bruker_uv;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

public class BrukerUvReader {

  private static final Logger logger = Logger.getLogger(BrukerUvReader.class.getName());
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(BrukerUvReader.class);

  private final File folder;
  private final File chromatographySqlite;

  private boolean initialized = false;

  public BrukerUvReader(File dFolder) {
    this.folder = dFolder;
    chromatographySqlite = new File(folder, "chromatography-data.sqlite");
  }

  public boolean hasUvData() {
    return chromatographySqlite.exists();
  }

  public boolean initialize() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      logger.info("Could not load sqlite.JDBC.");
      return false;
    }

    synchronized (org.sqlite.JDBC.class) {
      try (Connection connection = DriverManager.getConnection(
          "jdbc:sqlite:" + chromatographySqlite.getAbsolutePath())) {
        final List<Trace> traces = readTraceInfo(connection);
        logger.info("Loaded " + traces.size() + " traces.");
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    return true;
  }

  public static void main(String[] args) {
    BrukerUvReader reader = new BrukerUvReader(new File(
        "D:\\OneDrive - mzio GmbH\\mzio - shared\\Customer data\\Merck\\Bruker MS + Agilent HPLC\\ACC1_25105_3_blank_P1-C-1_1_2022_8102.d"));
    logger.info(reader.initialize()+ "");
  }

  private List<Trace> readTraceInfo(Connection conn) throws SQLException {
    final Statement statement = conn.createStatement();
    statement.setQueryTimeout(30);
    String query = "SELECT * FROM TraceSources";

    List<Trace> traces = new ArrayList<>();
    try (ResultSet rs = statement.executeQuery(query)) {
      final ResultSetMetaData metaData = rs.getMetaData();

      final Map<Integer, TraceColumns> indices = TraceColumns.findIndices(metaData);
      while (rs.next()) {
        var trace = new Trace();
        for (Entry<Integer, TraceColumns> entry : indices.entrySet()) {
          entry.getValue().map(entry.getKey(), rs, trace);
        }
        traces.add(trace);
      }
    }
    traces.removeIf(trace -> trace.type() == null || trace.type() == 9999L);
    return traces;
  }
}
