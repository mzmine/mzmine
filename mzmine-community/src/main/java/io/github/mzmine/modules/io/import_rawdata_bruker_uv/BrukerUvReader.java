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

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
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
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.CollectionUtils;
import java.io.File;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfDouble;
import java.lang.foreign.ValueLayout.OfFloat;
import java.nio.ByteOrder;
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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class BrukerUvReader implements AutoCloseable {

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
  private final Connection connection;
  private double traceProgress = 0d;
  private double spectraProgress = 0d;
  private String description = "Connecting to SQLite database...";

  private BrukerUvReader(File dFolder) throws SQLException {
    this.folder = dFolder;
    chromatographySqlite = new File(folder, "chromatography-data.sqlite");
    initSql();

    if (folder.exists()) {
      connection = DriverManager.getConnection(
          "jdbc:sqlite:" + chromatographySqlite.getAbsolutePath());
    } else {
      connection = null;
    }
  }

  public static boolean hasUvData(File dFolder) {
    if (new File(dFolder, "chromatography-data.sqlite").exists()) {
      return true;
    }
    return false;
  }

  public static BrukerUvReader forFolder(File dFolder) throws SQLException {
    return new BrukerUvReader(dFolder);
  }

  public static void loadAndAddForFile(File dFolder, RawDataFileImpl impl,
      MemoryMapStorage storage) {
    if (!hasUvData(dFolder)) {
      return;
    }

    try (var reader = forFolder(dFolder)) {
      final List<OtherDataFile> spectra = reader.loadSpectra(storage, impl);
      final List<OtherDataFile> traces = reader.loadChromatograms(storage, impl);

      impl.addOtherDataFiles(spectra);
      impl.addOtherDataFiles(traces);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
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

  /**
   * @param storage          The storage for the intensities.
   * @param resultSet        The sql query result. col 1 must be rt, col 2 a blob of intensities.
   * @param spectralData     spectral data to add the spectrum to.
   * @param storedDomainAxis Already mapped domain axis values. they are all the same for the whole
   *                         acquisition for PDA spectra.
   * @return The loaded spectrum.
   */
  private static @NotNull WavelengthSpectrum getSpectrumFromResult(MemoryMapStorage storage,
      ResultSet resultSet, OtherSpectralDataImpl spectralData, MemorySegment storedDomainAxis,
      double timeOffset) throws SQLException {
    final double rt = (resultSet.getDouble(1) + timeOffset) / 60;
    final byte[] intensitiesArray = resultSet.getBytes(2);
    final MemorySegment intensitiesSegment = MemorySegment.ofArray(intensitiesArray);

    // intensities are an array of floats
    final float[] intensities = intensitiesSegment.toArray(FLOAT_ARRAY_LAYOUT);
    assert intensities.length == StorageUtils.numDoubles(storedDomainAxis);

    final WavelengthSpectrum spectrum = new WavelengthSpectrum(spectralData, storedDomainAxis,
        StorageUtils.storeValuesToDoubleBuffer(storage,
            ConversionUtils.convertFloatsToDoubles(intensities)), MassSpectrumType.PROFILE,
        (float) rt);

    return spectrum;
  }

  private static void applyUnitLabelsToSpectralData(SpectrumSource detector,
      OtherSpectralDataImpl spectralData) {
    if (detector.yAxisUnit() != 7) {
      spectralData.setSpectraRangeLabel(BrukerUtils.unitToLabel(detector.yAxisUnit()));
      spectralData.setSpectraRangeUnit(BrukerUtils.unitToString(detector.yAxisUnit()));
    } else {
      spectralData.setSpectraRangeLabel("Intensity");
      spectralData.setSpectraRangeUnit("a.u.");
    }
    if (detector.xAxisUnit() != 7) {
      spectralData.setSpectraDomainLabel(BrukerUtils.unitToLabel(detector.xAxisUnit()));
      spectralData.setSpectraDomainUnit(BrukerUtils.unitToString(detector.xAxisUnit()));
    } else {
      spectralData.setSpectraDomainLabel("Wavelength");
      spectralData.setSpectraDomainUnit("nm");
    }
  }

  public boolean isConnected() throws SQLException {
    return connection != null && connection.isValid(2);
  }

  public boolean hasUvData() {
    return chromatographySqlite.exists();
  }

  /**
   * Loads chromatogram data from a sqlite connection.
   */
  public List<OtherDataFile> loadChromatograms(MemoryMapStorage storage, RawDataFile msFile)
      throws SQLException {

    final Map<ChromatogramType, List<Trace>> groupedChromatograms = readTraceInfo(connection);

    final long numTraces = groupedChromatograms.values().stream().mapToLong(List::size).sum();
    logger.finest("%s: Detected %d traces.".formatted(folder.getName(), numTraces));
    long importedTraces = 0;

    final List<OtherDataFile> groupedData = new ArrayList<>();

    for (Entry<ChromatogramType, List<Trace>> entryGroupedByChromType : groupedChromatograms.entrySet()) {
      final ChromatogramType type = entryGroupedByChromType.getKey();

      // unfortunately, grouping just by chrom type is not enough because temperature is also
      // marked as from UV detector...
      final Map<String, List<Trace>> groupedByUnit = ConversionUtils.groupByUnit(
          entryGroupedByChromType.getValue(), Trace::getConvertedRangeUnit);

      for (Entry<String, List<Trace>> entry : groupedByUnit.entrySet()) {
        final String unit = entry.getKey();

        final OtherDataFileImpl otherDataFile = new OtherDataFileImpl(msFile);
        otherDataFile.setDescription(unit + "_" + entry.getValue().getFirst().instrument());
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

    final List<Trace> traces = Trace.readFromSql(connection);

    traces.removeIf(trace -> !trace.isValid());
    return traces.stream().filter(trace -> !trace.getChomatogramType().isMsType())
        .collect(Collectors.groupingBy(Trace::getChomatogramType));
  }

  private OtherTimeSeries loadTimeSeriesData(MemoryMapStorage storage,
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

        final TDoubleArrayList timesList = new TDoubleArrayList();
        final TFloatArrayList intensitiesList = new TFloatArrayList();
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

        final double offset = Objects.requireNonNullElse(trace.timeOffset(), 0d);
        for (int i = 0; i < timesList.size(); i++) {
          timesList.set(i,
              (timesList.getQuick(i) + offset) / 60); // convert to minutes and apply offset
        }

        final List<DataPoint> dps = dropDuplicateRtDataPoints(trace, intensitiesList, timesList);

        return new SimpleOtherTimeSeries(storage, ConversionUtils.convertDoublesToFloats(
            dps.stream().mapToDouble(DataPoint::getMZ).toArray()),
            dps.stream().mapToDouble(DataPoint::getIntensity).toArray(), trace.description(),
            otherTimeSeriesData);
      }
    }
  }

  /**
   * Apparently it is not guaranteed that data points are unique or sorted. So we do that here.
   */
  private @NotNull List<DataPoint> dropDuplicateRtDataPoints(Trace trace,
      TFloatArrayList intensitiesList, TDoubleArrayList timesList) {
    List<DataPoint> dps = new ArrayList<>();
    for (int i = 0; i < intensitiesList.size(); i++) {
      final SimpleDataPoint dp = new SimpleDataPoint(timesList.get(i), intensitiesList.get(i));
      dps.add(dp);
    }

    // intermediate filtering to remove duplicates from traces. Why is this even necessary?
    dps.sort(DataPointSorter.DEFAULT_MZ_ASCENDING);
    final int before = dps.size();
    CollectionUtils.dropDuplicatesRetainOrder(dps);
    if (before != dps.size()) {
      logger.info(
          "%s - dropped %d duplicate values from chromatogram trace %d".formatted(folder.getName(),
              dps.size(), trace.id()));
    }
    return dps;
  }

  public List<OtherDataFile> loadSpectra(MemoryMapStorage storage, RawDataFile msFile)
      throws SQLException {
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
      final MemorySegment storedDomainAxis = StorageUtils.storeValuesToDoubleBuffer(storage,
          xAxisValues);

      final OtherDataFileImpl spectraFile = new OtherDataFileImpl(msFile);
      final OtherSpectralDataImpl spectralData = new OtherSpectralDataImpl(spectraFile);

      applyUnitLabelsToSpectralData(detector, spectralData);

      final double timeOffset = Objects.requireNonNullElse(detector.timeOffset(), 0d);

      try (Statement statement = connection.createStatement()) {
        statement.setQueryTimeout(30);
        final String spectraQuery = "SELECT Time,Intensities FROM Spectra WHERE Source=%s".formatted(
            detector.id());
        final ResultSet resultSet = statement.executeQuery(spectraQuery);

        while (resultSet.next()) {
          final WavelengthSpectrum spectrum = getSpectrumFromResult(storage, resultSet,
              spectralData, storedDomainAxis, timeOffset);
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

  @Override
  public void close() throws Exception {
    connection.close();
  }
}
