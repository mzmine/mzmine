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
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherSpectralDataImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.WavelengthSpectrum;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfDouble;
import java.lang.foreign.ValueLayout.OfFloat;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
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
import org.jetbrains.annotations.NotNull;

public class BrukerUvReader {

  private static final Logger logger = Logger.getLogger(BrukerUvReader.class.getName());
  private static final int TRACE_TIME_COL_INDEX = 1;
  private static final int TRACE_INTENSITY_COL_INDEX = 2;

  /**
   * Layout for reading y axis data. the Byte order is little endian. The aligmnent has to be set to
   * 1 (unaligned) to convert from the byte segment to a float array.
   */
  public static OfFloat FLOAT_ARRAY_LAYOUT = ValueLayout.JAVA_FLOAT.withOrder(
      ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);

  /**
   * Layout for reading x axis data. the Byte order is little endian. The aligmnent has to be set to
   * 1 (unaligned) to convert from the byte segment to a double array.
   */
  public static OfDouble DOUBLE_ARRAY_LAYOUT = ValueLayout.JAVA_DOUBLE.withOrder(
      ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);

  private final File folder;
  private final File chromatographySqlite;
  private double traceProgress = 0d;
  private double spectraProgress = 0d;
  private String description = "Connecting to SQLite database...";

  private boolean initialized = false;

  public BrukerUvReader(File dFolder) {
    this.folder = dFolder;
    chromatographySqlite = new File(folder, "chromatography-data.sqlite");
  }

  public static void main(String[] args) {
    BrukerUvReader reader = new BrukerUvReader(new File("I:\\Downloads"));
    final RawDataFileImpl file = new RawDataFileImpl("bla", null, null);
    reader.importOtherData(file, null);

    logger.finest("load" + file.getOtherDataFiles().size());
  }

  private static boolean initSql() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      logger.info("Could not load sqlite.JDBC.");
      return false;
    }
    return true;
  }

  private static @NotNull WavelengthSpectrum getSpectrumFromResult(MemoryMapStorage storage,
      ResultSet resultSet, OtherSpectralDataImpl spectralData, DoubleBuffer storedDomainAxis)
      throws SQLException {
    final double rt = resultSet.getDouble(1);
    final byte[] intensitiesArray = resultSet.getBytes(2);
    final MemorySegment intensitiesSegment = MemorySegment.ofArray(intensitiesArray);

    final float[] intensities = intensitiesSegment.toArray(FLOAT_ARRAY_LAYOUT);
    assert intensities.length == storedDomainAxis.limit() / 8;

    final WavelengthSpectrum spectrum = new WavelengthSpectrum(spectralData, storedDomainAxis,
        StorageUtils.storeValuesToDoubleBuffer(storage,
            ConversionUtils.convertFloatsToDoubles(intensities)), MassSpectrumType.PROFILE,
        (float) rt);
    return spectrum;
  }

  private static void applyUnitLabelsToSpectralData(SpectrumSource detector,
      OtherSpectralDataImpl spectralData) {
    spectralData.setSpectraDomainLabel(BrukerUtils.unitToLabel(detector.xAxisUnit()));
    spectralData.setSpectraDomainUnit(BrukerUtils.unitToString(detector.xAxisUnit()));
    spectralData.setSpectraRangeLabel(BrukerUtils.unitToLabel(detector.yAxisUnit()));
    spectralData.setSpectraRangeUnit(BrukerUtils.unitToString(detector.yAxisUnit()));
  }

  public boolean hasDataData() {
    return chromatographySqlite.exists();
  }

  public boolean importOtherData(RawDataFile file, MemoryMapStorage storage) {
    if (!initSql()) {
      return false;
    }

    synchronized (org.sqlite.JDBC.class) {
      try (Connection connection = DriverManager.getConnection(
          "jdbc:sqlite:" + chromatographySqlite.getAbsolutePath())) {

        description = "Importing traces";
        final List<OtherDataFile> otherTraceFiles = loadChromatograms(connection, storage, file);
        final List<OtherDataFile> otherSpectraFiles = loadSpectra(connection, storage, file);
        if (file instanceof RawDataFileImpl impl) {
          impl.addOtherDataFiles(otherTraceFiles);
          impl.addOtherDataFiles(otherSpectraFiles);
        }

      } catch (SQLException e) {
        logger.log(Level.SEVERE,
            "Cannot load chromatography data for file %s".formatted(folder.getName()), e);
        return false;
      }
    }

    return true;
  }

  /**
   * Loads chromatogram data from an sqllite connection.
   *
   * @param connection
   * @param storage
   * @param msFile
   * @return
   * @throws SQLException
   */
  public List<OtherDataFile> loadChromatograms(Connection connection, MemoryMapStorage storage,
      RawDataFile msFile) throws SQLException {

    final Map<ChromatogramType, List<Trace>> groupedChromatograms = readTraceInfo(connection);

    final long numTraces = groupedChromatograms.values().stream().mapToLong(List::size).sum();
    logger.finest("%s: Detected %d traces.".formatted(folder.getName(), numTraces));
    long importedTraces = 0;

    final List<OtherDataFile> groupedData = new ArrayList<>();

    for (Entry<ChromatogramType, List<Trace>> entry : groupedChromatograms.entrySet()) {
      final ChromatogramType type = entry.getKey();
      final OtherDataFileImpl otherDataFile = new OtherDataFileImpl(msFile);
      final OtherTimeSeriesDataImpl timeSeriesData = new OtherTimeSeriesDataImpl(otherDataFile);
      timeSeriesData.setChromatogramType(type);

      logger.finest("%s: Importing time series data for traces.".formatted(folder.getName()));
      for (Trace trace : entry.getValue()) {
        final OtherTimeSeries timeSeries = loadTimeSeriesData(storage, timeSeriesData, trace,
            connection);
        timeSeriesData.setTimeSeriesRangeLabel(trace.getConvertedRangeLabel());
        timeSeriesData.setTimeSeriesRangeUnit(trace.getConvertedRangeUnit());

        final OtherFeature feature = new OtherFeatureImpl(timeSeries);
        timeSeriesData.addRawTrace(feature);

        importedTraces++;
        traceProgress = (double) importedTraces / numTraces;
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
    description = "Reading trace information.";
    logger.finest("%s: Reading trace information".formatted(folder.getName()));

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
    return traces.stream().filter(trace -> !trace.getChomatogramType().isMsType())
        .collect(Collectors.groupingBy(Trace::getChomatogramType));
  }

  public OtherTimeSeries loadTimeSeriesData(MemoryMapStorage storage,
      OtherTimeSeriesData otherTimeSeriesData, final Trace trace, Connection connection)
      throws SQLException {

    description = "Importing traces.";
    try (final Statement statement = connection.createStatement()) {

      final String query = "SELECT Times,Intensities FROM TraceChunks WHERE Trace=%d".formatted(
          trace.id());
      statement.setQueryTimeout(30);

      try (final ResultSet results = statement.executeQuery(query)) {
        final ResultSetMetaData metaData = results.getMetaData();
        if (!metaData.getColumnName(TRACE_TIME_COL_INDEX).equals("Times")
            && !metaData.getColumnName(TRACE_INTENSITY_COL_INDEX).equals("Intensities")) {
          throw new IllegalStateException(
              "Cannot find Times and Intensities columns in sqlite data of %s.".formatted(
                  folder.getName()));
        }

        TDoubleArrayList timesList = new TDoubleArrayList();
        TFloatArrayList intensitiesList = new TFloatArrayList();
        while (results.next()) {
          final MemorySegment timesSegment = MemorySegment.ofArray(
              results.getBytes(TRACE_TIME_COL_INDEX));
          final MemorySegment intensitiesSegment = MemorySegment.ofArray(
              results.getBytes(TRACE_INTENSITY_COL_INDEX));

          if (timesSegment.byteSize() / 8 != intensitiesSegment.byteSize() / 4) {
            throw new IllegalArgumentException(
                "Number of time (%d) and intensity (%d) values does not match.".formatted(
                    timesSegment.byteSize() / 8, intensitiesSegment.byteSize() / 4));
          }

          timesList.addAll(timesSegment.toArray(DOUBLE_ARRAY_LAYOUT));
          intensitiesList.addAll(intensitiesSegment.toArray(FLOAT_ARRAY_LAYOUT));
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

  private List<OtherDataFile> loadSpectra(Connection connection, MemoryMapStorage storage,
      RawDataFile msFile) throws SQLException {
    description = "Reading spectra information";
    logger.finest("%s: Reading spectra information".formatted(folder.getName()));

    List<OtherDataFile> otherDataFiles = new ArrayList<>();
    final List<SpectrumSource> detectors = SpectrumSource.loadFromSqlite(connection);
    logger.finest(
        "%s: Detected %d spectrum sources.".formatted(folder.getName(), detectors.size()));

    final long numSpectra = detectors.stream().mapToLong(SpectrumSource::numSpectra).sum();
    long spectraCounter = 0L;

    description = "Reading spectra.";
    for (SpectrumSource detector : detectors) {
      final double[] xAxisValues = detector.xAxis();
      final DoubleBuffer storedDomainAxis = StorageUtils.storeValuesToDoubleBuffer(storage,
          xAxisValues);
      // load 100 spectra at a time

      final OtherDataFileImpl spectraFile = new OtherDataFileImpl(msFile);
      final OtherSpectralDataImpl spectralData = new OtherSpectralDataImpl(spectraFile);

      applyUnitLabelsToSpectralData(detector, spectralData);

      try (Statement statement = connection.createStatement()) {
        statement.setQueryTimeout(30);
        final String spectraQuery = "SELECT Time,Intensities FROM Spectra WHERE Source=%s".formatted(
            detector.id());
        final ResultSet resultSet = statement.executeQuery(spectraQuery);

        while (resultSet.next()) {
          final WavelengthSpectrum spectrum = getSpectrumFromResult(storage, resultSet,
              spectralData, storedDomainAxis);
          spectralData.addSpectrum(spectrum);

          spectraCounter++;
          spectraProgress = (double) spectraCounter / numSpectra;
        }

        resultSet.close();
      }
      if (!spectralData.getSpectra().isEmpty()) {
        spectraFile.setOtherSpectralData(spectralData);
        otherDataFiles.add(spectraFile);
      }

    }
    return otherDataFiles;
  }

  public double getProgress() {
    return traceProgress * 0.3 + spectraProgress * 0.7;
  }

  public String getDescription() {
    return description;
  }
}
