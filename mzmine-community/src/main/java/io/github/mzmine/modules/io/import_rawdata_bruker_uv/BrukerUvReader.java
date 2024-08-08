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

package io.github.mzmine.modules.io.import_rawdata_bruker_uv;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

  public static void main(String[] args) {
    BrukerUvReader reader = new BrukerUvReader(new File(
        "J:\\Home\\AK_Karst\\Messdaten\\Aktive\\nmarczin\\TOF\\20270718\\DNPHMixNC4EArGlu_1uM_RB1_5792.d"));
    logger.info(reader.initialize() + "");
  }

  public boolean hasUvData() {
    return chromatographySqlite.exists();
  }

  public boolean importOtherData(RawDataFile file, MemoryMapStorage storage) {
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
        final List<OtherDataFile> otherDataFiles = loadChromatograms(connection, storage, file);
        if (file instanceof RawDataFileImpl impl) {
          impl.addOtherDataFiles(otherDataFiles);
        }


      } catch (SQLException e) {
        logger.log(Level.SEVERE,
            "Cannot load chromatography data for file %s".formatted(folder.getName()), e);
        return false;
      }
    }

    return true;
  }

  public List<OtherDataFile> loadChromatograms(Connection connection, MemoryMapStorage storage,
      RawDataFile msFile) throws SQLException {
    final Map<ChromatogramType, List<Trace>> groupedChromatograms = readTraceInfo(connection);

    List<OtherDataFile> groupedData = new ArrayList<>();

    for (Entry<ChromatogramType, List<Trace>> entry : groupedChromatograms.entrySet()) {
      final ChromatogramType type = entry.getKey();
      final OtherDataFileImpl otherDataFile = new OtherDataFileImpl(msFile);
      final OtherTimeSeriesDataImpl timeSeriesData = new OtherTimeSeriesDataImpl(otherDataFile);
      timeSeriesData.setChromatogramType(type);

      for (Trace trace : entry.getValue()) {
        final OtherTimeSeries timeSeries = loadTimeSeriesData(storage, timeSeriesData, trace,
            connection);
        timeSeriesData.setTimeSeriesRangeLabel(trace.getLabel());
        timeSeriesData.setTimeSeriesRangeLabel(trace.getUnit());
        final OtherFeature feature = new OtherFeatureImpl(timeSeries);
        timeSeriesData.addRawTrace(feature);
      }

      otherDataFile.setOtherTimeSeriesData(timeSeriesData);
      groupedData.add(otherDataFile);
    }

    return groupedData;
  }

  /**
   * Reads chromatograms from the sqlite file and groups them by {@link ChromatogramType}.
   */
  private Map<ChromatogramType, List<Trace>> readTraceInfo(Connection connection)
      throws SQLException {
    final Statement statement = connection.createStatement();
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

    traces.removeIf(trace -> !trace.isValid());
    return traces.stream().collect(Collectors.groupingBy(Trace::getChomatogramType));
  }

  public OtherTimeSeries loadTimeSeriesData(MemoryMapStorage storage,
      OtherTimeSeriesData otherTimeSeriesData, final Trace trace, Connection connection)
      throws SQLException {
    final Statement statement = connection.createStatement();

    final String query = "SELECT Times,Intensities FROM TraceChunks WHERE Trace=%d".formatted(
        trace.id());
    statement.setQueryTimeout(30);

    try (final ResultSet results = statement.executeQuery(query)) {
      final ResultSetMetaData metaData = results.getMetaData();
      if (!metaData.getColumnName(0).equals("Times") && !metaData.getColumnName(1)
          .equals("Intensities")) {
        throw new IllegalStateException(
            "Cannot find Times and Intensities columns in sqlite data of %s.".formatted(
                folder.getName()));
      }

      TDoubleArrayList timesList = new TDoubleArrayList();
      TFloatArrayList intensitiesList = new TFloatArrayList();
      while (results.next()) {
        final MemorySegment timesSegment = MemorySegment.ofArray(results.getBytes(0));
        final MemorySegment intensitiesSegment = MemorySegment.ofArray(results.getBytes(1));

        if (timesSegment.byteSize() / 8 != intensitiesSegment.byteSize() / 4) {
          throw new IllegalArgumentException(
              "Number of time (%d) and intensity (%d) values does not match.".formatted(
                  timesSegment.byteSize() / 8, intensitiesSegment.byteSize() / 4));
        }

        timesSegment.toArray(ValueLayout.JAVA_DOUBLE);
        intensitiesSegment.toArray(ValueLayout.JAVA_FLOAT);

        timesList.addAll(timesSegment.toArray(ValueLayout.JAVA_DOUBLE));
        intensitiesList.addAll(intensitiesSegment.toArray(ValueLayout.JAVA_FLOAT));
      }

      if (timesList.size() != intensitiesList.size()) {
        throw new IllegalArgumentException(
            "Number of times and intensities values does not match.");
      }

      return new SimpleOtherTimeSeries(storage,
          ConversionUtils.convertDoublesToFloats(timesList.toArray()),
          ConversionUtils.convertFloatsToDoubles(intensitiesList.toArray()), trace.description(),
          otherTimeSeriesData);
    }
  }
}
