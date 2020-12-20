package io.github.mzmine.project.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;
import io.github.mzmine.util.javafx.FxColorUtil;
import javafx.beans.property.ObjectProperty;
import javafx.scene.paint.Color;


public class ImagingRawDataFileImpl implements ImagingRawDataFile, RawDataFileWriter {

  // imaging parameters
  private ImagingParameters imagingParameters;
  public static final String SAVE_IDENTIFIER = "Imaging raw data file";
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final Hashtable<Integer, Range<Double>> dataMZRange;
  private final Hashtable<Integer, int[]> scanNumbersCache;
  private final Hashtable<Integer, Double> dataMaxBasePeakIntensity;
  private final Hashtable<Integer, Double> dataMaxTIC;

  private ByteBuffer buffer = ByteBuffer.allocate(20000);
  private final TreeMap<Integer, Long> dataPointsOffsets;
  private final TreeMap<Integer, Integer> dataPointsLengths;

  private String dataFileName;

  // Temporary file for scan data storage
  private File dataPointsFileName;
  private RandomAccessFile dataPointsFile;

  private final Hashtable<Integer, StorableImagingScan> scans;
  private ObjectProperty<Color> color;


  public ImagingRawDataFileImpl(String dataFileName) throws IOException {
    this.dataFileName = dataFileName;
    scanNumbersCache = new Hashtable<>();
    dataMZRange = new Hashtable<>();
    dataMaxBasePeakIntensity = new Hashtable<>();
    dataMaxTIC = new Hashtable<>();
    dataPointsOffsets = new TreeMap<Integer, Long>();
    dataPointsLengths = new TreeMap<Integer, Integer>();

    scans = new Hashtable<>();
  }

  public synchronized void addScan(Scan newScan) throws IOException {

    if (newScan instanceof StorableImagingScan) {
      scans.put(newScan.getScanNumber(), (StorableImagingScan) newScan);
      return;
    }

    final int storageID = storeDataPoints(newScan.getDataPoints());
    StorableImagingScan storedScan =
        new StorableImagingScan(newScan, this, newScan.getNumberOfDataPoints(), storageID);
    scans.put(storedScan.getScanNumber(), storedScan);
    if (scans.put(newScan.getScanNumber(), storedScan) != null) {
      logger.info("scan " + newScan.getScanNumber() + " already existed");
    }
  }

  public synchronized int storeDataPoints(DataPoint dataPoints[]) throws IOException {
    if (dataPointsFile == null) {
      File newFile = ImagingRawDataFileImpl.createNewDataPointsFile();
      openDataPointsFile(newFile);
    }
    final long currentOffset = dataPointsFile.length();

    final int currentID;
    if (!dataPointsOffsets.isEmpty()) {
      currentID = dataPointsOffsets.lastKey() + 1;
    } else {
      currentID = 1;
    }
    final int numOfDataPoints = dataPoints.length;

    // Convert the dataPoints into a byte array. Each double takes 8 bytes,
    // so we get the current double offset by dividing the size of the file
    // by 8
    final int numOfBytes = numOfDataPoints * 2 * 8;

    if (buffer.capacity() < numOfBytes) {
      buffer = ByteBuffer.allocate(numOfBytes * 2);
    } else {
      // JDK 9 breaks compatibility with JRE8: need to cast
      // https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime
      ((Buffer) buffer).clear();
    }

    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
    for (DataPoint dp : dataPoints) {
      doubleBuffer.put((double) dp.getMZ());
      doubleBuffer.put((double) dp.getIntensity());
    }

    dataPointsFile.seek(currentOffset);
    dataPointsFile.write(buffer.array(), 0, numOfBytes);

    dataPointsOffsets.put(currentID, currentOffset);
    dataPointsLengths.put(currentID, numOfDataPoints);

    return currentID;

  }

  /**
   * Opens the given file as a data points file for this RawDataFileImpl instance. If the file is
   * not empty, the TreeMaps supplied as parameters have to describe the mapping of storage IDs to
   * data points in the file.
   */
  public synchronized void openDataPointsFile(File dataPointsFileName) throws IOException {
    if (this.dataPointsFile != null) {
      throw new IOException("Cannot open another data points file, because one is already open");
    }
    this.dataPointsFileName = dataPointsFileName;
    this.dataPointsFile = new RandomAccessFile(dataPointsFileName, "rw");

    // Locks the temporary file so it is not removed when another instance
    // of MZmine is starting. Lock will be automatically released when this
    // instance of MZmine exits. Locking may fail on network-mounted
    // filesystems.
    try {
      FileChannel fileChannel = dataPointsFile.getChannel();
      fileChannel.lock();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to lock the file " + dataPointsFileName, e);
    }

    // Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
    // bug #4171239. We will try to remove the temporary files in a
    // shutdown hook registered in the main.ShutDownHook class
    dataPointsFileName.deleteOnExit();
  }


  /**
   * Create a new temporary data points file
   */
  public static File createNewDataPointsFile() throws IOException {
    return File.createTempFile("mzmine", ".scans");
  }

  @Override
  public void setImagingParam(ImagingParameters imagingParameters) {
    this.imagingParameters = imagingParameters;
  }

  @Override
  public ImagingParameters getImagingParam() {
    return imagingParameters;
  }

  @Override
  public RawDataFile clone() throws CloneNotSupportedException {
    return (RawDataFile) super.clone();
  }

  @Override
  @Nonnull
  public String getName() {
    return dataFileName;
  }

  @Override
  public void setName(@Nonnull String name) {
    this.dataFileName = name;
  }

  @Override
  public int getNumOfScans() {
    return scans.size();
  }

  @Override
  public int getNumOfScans(int msLevel) {
    return getScanNumbers(msLevel).length;
  }

  @Override
  public int[] getMSLevels() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public @Nonnull int[] getScanNumbers(int msLevel) {
    if (scanNumbersCache.containsKey(msLevel)) {
      return scanNumbersCache.get(msLevel);
    }
    Range<Float> all = Range.all();
    int scanNumbers[] = getScanNumbers(msLevel, all);
    scanNumbersCache.put(msLevel, scanNumbers);
    return scanNumbers;
  }

  @Override
  @Nonnull
  public int[] getScanNumbers() {
    if (scanNumbersCache.containsKey(0) && scanNumbersCache.get(0).length == scans.size()) {
      return scanNumbersCache.get(0);
    }
    Set<Integer> allScanNumbers = scans.keySet();
    int[] numbersArray = Ints.toArray(allScanNumbers);
    Arrays.sort(numbersArray);
    scanNumbersCache.put(0, numbersArray);
    return numbersArray;
  }


  @Override
  public int[] getScanNumbers(int msLevel, Range<Float> rtRange) {
    return new int[0];
  }

  @Override
  public @Nullable Scan getScan(int scanNumber) {
    return scans.get(scanNumber);
  }

  @Override
  public int getScanNumberAtRT(float rt, int mslevel) {
    return -1;
  }

  @Override
  public int getScanNumberAtRT(float rt) {
    return -1;
  }

  @Override
  @Nonnull
  public Range<Double> getDataMZRange() {
    return getDataMZRange(0);
  }

  @Override
  public Range<Float> getDataRTRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getDataMZRange(int msLevel) {
    // check if we have this value already cached
    Range<Double> mzRange = dataMZRange.get(msLevel);
    if (mzRange != null) {
      return mzRange;
    }
    // find the value
    for (Scan scan : scans.values()) {
      // ignore scans of other ms levels
      if ((msLevel != 0) && (scan.getMSLevel() != msLevel)) {
        continue;
      }
      if (mzRange == null) {
        mzRange = scan.getDataPointMZRange();
      } else {
        mzRange = mzRange.span(scan.getDataPointMZRange());
      }
    }
    // cache the value, if we found any
    if (mzRange != null) {
      dataMZRange.put(msLevel, mzRange);
    } else {
      mzRange = Range.singleton(0.0);
    }
    return mzRange;
  }

  @Override
  public Range<Float> getDataRTRange(Integer msLevel) {
    return null;
  }

  @Override
  public double getDataMaxBasePeakIntensity(int msLevel) {
    // check if we have this value already cached
    Double maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
    if (maxBasePeak != null) {
      return maxBasePeak;
    }
    // find the value
    Enumeration<StorableImagingScan> scansEnum = scans.elements();
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();
      // ignore scans of other ms levels
      if (scan.getMSLevel() != msLevel) {
        continue;
      }
      DataPoint scanBasePeak = scan.getHighestDataPoint();
      if (scanBasePeak == null) {
        continue;
      }
      if ((maxBasePeak == null) || (scanBasePeak.getIntensity() > maxBasePeak)) {
        maxBasePeak = scanBasePeak.getIntensity();
      }
    }
    // return -1 if no scan at this MS level
    if (maxBasePeak == null) {
      maxBasePeak = -1d;
    }
    // cache the value
    dataMaxBasePeakIntensity.put(msLevel, maxBasePeak);
    return maxBasePeak;
  }

  @Override
  public double getDataMaxTotalIonCurrent(int msLevel) {
    // check if we have this value already cached
    Double maxTIC = dataMaxTIC.get(msLevel);
    if (maxTIC != null) {
      return maxTIC.doubleValue();
    }
    // find the value
    Enumeration<StorableImagingScan> scansEnum = scans.elements();
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();
      // ignore scans of other ms levels
      if (scan.getMSLevel() != msLevel) {
        continue;
      }
      if ((maxTIC == null) || (scan.getTIC() > maxTIC)) {
        maxTIC = scan.getTIC();
      }
    }
    // return -1 if no scan at this MS level
    if (maxTIC == null) {
      maxTIC = -1d;
    }
    // cache the value
    dataMaxTIC.put(msLevel, maxTIC);
    return maxTIC;
  }

  @Override
  public List<PolarityType> getDataPolarity() {
    Enumeration<StorableImagingScan> scansEnum = scans.elements();
    EnumSet<PolarityType> polarityTypes = EnumSet.noneOf(PolarityType.class);
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();
      polarityTypes.add(scan.getPolarity());
    }
    // return as list
    return polarityTypes.stream().collect(Collectors.toList());
  }

  @Override
  public java.awt.Color getColorAWT() {
    return FxColorUtil.fxColorToAWT(color.getValue());
  }

  @Override
  public javafx.scene.paint.Color getColor() {
    return color.getValue();
  }

  @Override
  public void setColor(javafx.scene.paint.Color color) {
    this.color.setValue(color);
  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> colorProperty() {
    return color;
  }

  @Override
  public synchronized void close() {
    try {
      if (dataPointsFileName != null) {
        dataPointsFile.close();
        dataPointsFileName.delete();
      }
    } catch (IOException e) {
      logger.warning("Could not close file " + dataPointsFileName + ": " + e.toString());
    }
  }

  @Override
  public String toString() {
    return dataFileName;
  }

  @Override
  public synchronized RawDataFile finishWriting() throws IOException {
    for (StorableImagingScan scan : scans.values()) {
      scan.updateValues();
    }
    logger.finest("Writing of scans to file " + dataPointsFileName + " finished");
    return this;
  }
}
