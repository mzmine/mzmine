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

package io.github.mzmine.modules.io.import_rawdata_imzml;

import com.alanmrace.jimzmlparser.imzml.ImzML;
import com.alanmrace.jimzmlparser.mzml.BinaryDataArray;
import com.alanmrace.jimzmlparser.mzml.BinaryDataArrayList;
import com.alanmrace.jimzmlparser.mzml.CVParam;
import com.alanmrace.jimzmlparser.mzml.Precursor;
import com.alanmrace.jimzmlparser.mzml.PrecursorList;
import com.alanmrace.jimzmlparser.mzml.Scan;
import com.alanmrace.jimzmlparser.mzml.ScanList;
import com.alanmrace.jimzmlparser.mzml.SelectedIon;
import com.alanmrace.jimzmlparser.mzml.SelectedIonList;
import com.alanmrace.jimzmlparser.mzml.Spectrum;
import com.alanmrace.jimzmlparser.mzml.SpectrumList;
import com.alanmrace.jimzmlparser.parser.ImzMLHandler;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleImagingScan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class ImzMLImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(ImzMLImportTask.class.getName());

  private final File file;
  private final MZmineProject project;
  private final ScanImportProcessorConfig scanProcessorConfig;
  private final ImagingRawDataFile newMZmineFile;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private int totalScans = 0, parsedScans;

  private int lastScanNumber = 0;

  private final Map<String, Integer> scanIdTable = new Hashtable<>();

  /*
   * This stack stores at most 20 consecutive scans. This window serves to find possible fragments
   * (current scan) that belongs to any of the stored scans in the stack. The reason of the size
   * follows the concept of neighborhood of scans and all his fragments. These solution is
   * implemented because exists the possibility to find fragments of one scan after one or more full
   * scans.
   */
  private static final int PARENT_STACK_SIZE = 20;
  private final LinkedList<SimpleScan> parentStack = new LinkedList<>();

  public ImzMLImportTask(MZmineProject project, File fileToOpen,
      final @NotNull ScanImportProcessorConfig scanProcessorConfig,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, final @Nullable MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage also set in raw data file
    this.project = project;
    this.file = fileToOpen;
    this.scanProcessorConfig = scanProcessorConfig;
    this.newMZmineFile = new ImagingRawDataFileImpl(file.getName(), file.getAbsolutePath(),
        getMemoryMapStorage());
    this.parameters = parameters;
    this.module = module;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started parsing file " + file);

    ImzML imzml;
    try {
      imzml = ImzMLHandler.parseimzML(file.getAbsolutePath());

      SpectrumList spectra = imzml.getRun().getSpectrumList();
      totalScans = spectra.size();
      for (int i = 0; i < totalScans; i++) {

        if (isCanceled()) {
          return;
        }

        Spectrum spectrum = spectra.get(i);

        // Ignore scans that are not MS, e.g. UV
        if (!isMsSpectrum(spectrum)) {
          parsedScans++;
          continue;
        }

        String scanId = spectrum.getID();
        int scanNumber = convertScanIdToScanNumber(scanId);

        // Extract scan data
        int msLevel = extractMSLevel(spectrum);
        float retentionTime = extractRetentionTime(spectrum);
        PolarityType polarity = extractPolarity(spectrum);
        int parentScan = extractParentScanNumber(spectrum);
        double precursorMz = extractPrecursorMz(spectrum);
        int precursorCharge = extractPrecursorCharge(spectrum);
        String scanDefinition = extractScanDefinition(spectrum);
        // imaging
        Coordinates coord = extractCoordinates(spectrum);

        // TODO find out if spectrum type is encoded in imzml file
        var metadataScan = new SimpleBuildingScan(scanNumber, msLevel, polarity,
            MassSpectrumType.CENTROIDED, retentionTime, precursorMz, precursorCharge);
        if (!scanProcessorConfig.scanFilter().matches(metadataScan)) {
          // skip parsing of data and skip this scan completely
          parsedScans++;
          continue;
        }

        double[] mzValues = extractMzValues(spectrum);
        double[] intensityValues = extractIntensityValues(spectrum);
        // Auto-detect whether this scan is centroided
        SimpleSpectralArrays data = new SimpleSpectralArrays(mzValues, intensityValues);
        MassSpectrumType spectrumType = ScanUtils.detectSpectrumType(mzValues, intensityValues);

        data = scanProcessorConfig.processor().processScan(metadataScan, data);

        if (scanProcessorConfig.isMassDetectActive(msLevel)) {
          spectrumType = MassSpectrumType.CENTROIDED;
        }

        SimpleImagingScan scan = new SimpleImagingScan(newMZmineFile, scanNumber, msLevel,
            retentionTime, precursorMz, precursorCharge, data.mzs(), data.intensities(),
            spectrumType, polarity, scanDefinition, null, coord);

        if (scanProcessorConfig.isMassDetectActive(msLevel)) {
          scan.addMassList(new ScanPointerMassList(scan));
        }

        /*
         * Verify the size of parentStack. The actual size of the window to cover possible
         * candidates is defined by limitSize.
         */
        if (parentStack.size() > PARENT_STACK_SIZE) {
          io.github.mzmine.datamodel.Scan firstScan = parentStack.removeLast();
          newMZmineFile.addScan(firstScan);
        }

        parentStack.addFirst(scan);

        parsedScans++;

      }

      while (!parentStack.isEmpty()) {
        io.github.mzmine.datamodel.Scan scan = parentStack.removeLast();
        newMZmineFile.addScan(scan);
      }
      newMZmineFile.getScans().sort(io.github.mzmine.datamodel.Scan::compareTo);

      // set settings of image
      newMZmineFile.setImagingParam(new ImagingParameters(imzml));
      newMZmineFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
      project.addFile(newMZmineFile);

    } catch (Throwable e) {
      logger.log(Level.WARNING, "Error in imzML import: " + e.getMessage(), e);
      error("Error parsing imzML: " + ExceptionUtils.exceptionToString(e));
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);

  }

  private int convertScanIdToScanNumber(String scanId) {

    if (scanIdTable.containsKey(scanId)) {
      return scanIdTable.get(scanId);
    }

    final Pattern pattern = Pattern.compile("scan=([0-9]+)");
    final Matcher matcher = pattern.matcher(scanId);
    boolean scanNumberFound = matcher.find();

    // Some vendors include scan=XX in the ID, some don't, such as
    // mzML converted from WIFF files. See the definition of nativeID in
    // http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo
    if (scanNumberFound) {
      int scanNumber = Integer.parseInt(matcher.group(1));
      scanIdTable.put(scanId, scanNumber);
      return scanNumber;
    }

    int scanNumber = lastScanNumber + 1;
    lastScanNumber++;
    scanIdTable.put(scanId, scanNumber);
    return scanNumber;
  }

  private int extractMSLevel(Spectrum spectrum) {
    // Browse the spectrum parameters
    // MS level MS:1000511
    // CVParam param = spectrum.getCVParam(Spectrum.MS1_SPECTRUM_ID);
    // if (param != null)
    // return param.getValueAsInteger();
    return 1;
  }

  private float extractRetentionTime(Spectrum spectrum) {
    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement == null) {
      return 0;
    }

    for (Scan scan : scanListElement) {
      try {
        // scan start time correct?
        CVParam param = scan.getCVParam(Scan.SCAN_START_TIME_ID);
        if (param != null) {
          return (float) param.getValueAsDouble();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return 0;
  }

  private double[] extractIntensityValues(Spectrum spectrum) {
    try {
      BinaryDataArrayList dataList = spectrum.getBinaryDataArrayList();
      BinaryDataArray intensityArray = dataList.getIntensityArray();
      double intensityValues[] = intensityArray.getDataAsDouble();
      return intensityValues;
    } catch (IOException e) {
      e.printStackTrace();
      return new double[0];
    }
  }

  private double[] extractMzValues(Spectrum spectrum) {
    try {
      BinaryDataArrayList dataList = spectrum.getBinaryDataArrayList();

      BinaryDataArray mzArray = dataList.getmzArray();
      double mzValues[] = mzArray.getDataAsDouble();
      return mzValues;

    } catch (IOException e) {
      e.printStackTrace();
      return new double[0];
    }

  }

  private Coordinates extractCoordinates(Spectrum spectrum) {
    ScanList list = spectrum.getScanList();
    if (list != null) {
      for (Scan scan : spectrum.getScanList()) {
        CVParam xValue = scan.getCVParam(Scan.POSITION_X_ID);
        CVParam yValue = scan.getCVParam(Scan.POSITION_Y_ID);
        CVParam zValue = scan.getCVParam(Scan.POSITION_Z_ID);

        if (xValue != null && yValue != null) {
          int x = xValue.getValueAsInteger() - 1;
          int y = yValue.getValueAsInteger() - 1;

          if (zValue != null) {
            return new Coordinates(x, y, zValue.getValueAsInteger() - 1);
          } else {
            return new Coordinates(x, y, 0);
          }
        }
      }
    }
    return null;
  }


  private int extractParentScanNumber(Spectrum spectrum) {
    PrecursorList precursorListElement = spectrum.getPrecursorList();
    if ((precursorListElement == null) || (precursorListElement.size() == 0)) {
      return -1;
    }

    for (Precursor parent : precursorListElement) {
      // Get the precursor scan number
      String precursorScanId = parent.getXMLAttributeText();
      if (precursorScanId == null) {
        return -1;
      }
      int parentScan = convertScanIdToScanNumber(precursorScanId);
      return parentScan;
    }
    return -1;
  }

  private double extractPrecursorMz(Spectrum spectrum) {

    PrecursorList precursorListElement = spectrum.getPrecursorList();
    if ((precursorListElement == null) || (precursorListElement.size() == 0)) {
      return 0;
    }

    for (Precursor parent : precursorListElement) {

      SelectedIonList selectedIonListElement = parent.getSelectedIonList();
      if ((selectedIonListElement == null) || (selectedIonListElement.size() == 0)) {
        return 0;
      }

      // MS:1000040 is used in mzML 1.0,
      // MS:1000744 is used in mzML 1.1.0
      for (SelectedIon sion : selectedIonListElement) {
        CVParam param = sion.getCVParam("MS:1000040");
        if (param != null) {
          return param.getValueAsDouble();
        }

        param = sion.getCVParam("MS:1000744");
        if (param != null) {
          return param.getValueAsDouble();
        }
      }
    }
    return 0;
  }

  private int extractPrecursorCharge(Spectrum spectrum) {
    PrecursorList precursorList = spectrum.getPrecursorList();
    if ((precursorList == null) || (precursorList.size() == 0)) {
      return 0;
    }

    for (Precursor parent : precursorList) {
      SelectedIonList selectedIonListElement = parent.getSelectedIonList();
      if ((selectedIonListElement == null) || (selectedIonListElement.size() == 0)) {
        return 0;
      }

      for (SelectedIon sion : selectedIonListElement) {

        // precursor charge
        CVParam param = sion.getCVParam("MS:1000041");
        if (param != null) {
          return param.getValueAsInteger();
        }
      }
    }
    return 0;
  }

  private PolarityType extractPolarity(Spectrum spectrum) {
    CVParam cv = spectrum.getCVParam(Spectrum.SCAN_POLARITY_ID);
    if (spectrum.getCVParam("MS:1000130") != null) {
      return PolarityType.POSITIVE;
    } else if (spectrum.getCVParam("MS:1000129") != null) {
      return PolarityType.NEGATIVE;
    }

    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement != null) {
      for (int i = 0; i < scanListElement.size(); i++) {
        Scan scan = scanListElement.get(i);

        if (scan.getCVParam("MS:1000130") != null) {
          return PolarityType.POSITIVE;
        } else if (scan.getCVParam("MS:1000129") != null) {
          return PolarityType.NEGATIVE;
        }
      }
    }
    return PolarityType.UNKNOWN;
  }

  private String extractScanDefinition(Spectrum spectrum) {
    CVParam cvParams = spectrum.getCVParam("MS:1000512");
    if (cvParams != null) {
      return cvParams.getValueAsString();
    }

    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement != null) {
      for (int i = 0; i < scanListElement.size(); i++) {
        Scan scan = scanListElement.get(i);

        cvParams = scan.getCVParam("MS:1000512");
        if (cvParams != null) {
          return cvParams.getValueAsString();
        }
      }
    }
    return spectrum.getID();
  }

  @Override
  public String getTaskDescription() {
    return "Opening file " + file;
  }

  boolean isMsSpectrum(Spectrum spectrum) {
    // one thats not MS (code for UV?)
    CVParam cvParams = spectrum.getCVParam("MS:1000804");

    // By default, let's assume unidentified spectra are MS spectra
    return cvParams == null;
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? newMZmineFile : null;
  }
}
