/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.deprecated_jmzml;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArrayList;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.ParamGroup;
import uk.ac.ebi.jmzml.model.mzml.Precursor;
import uk.ac.ebi.jmzml.model.mzml.PrecursorList;
import uk.ac.ebi.jmzml.model.mzml.Scan;
import uk.ac.ebi.jmzml.model.mzml.ScanList;
import uk.ac.ebi.jmzml.model.mzml.SelectedIonList;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.xml.io.MzMLObjectIterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class MzMLImportTask extends AbstractTask {

  private static final Pattern SCAN_PATTERN = Pattern.compile("scan=([0-9]+)");
  /*
   * This stack stores at most 20 consecutive scans. This window serves to find possible fragments
   * (current scan) that belongs to any of the stored scans in the stack. The reason of the size
   * follows the concept of neighborhood of scans and all his fragments. These solution is
   * implemented because exists the possibility to find fragments of one scan after one or more full
   * scans.
   */
  private static final int PARENT_STACK_SIZE = 20;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private IMSRawDataFile newImsFile;
  private int totalScans = 0, parsedScans;
  private int lastScanNumber = 0;
  private Map<String, Integer> scanIdTable = new Hashtable<String, Integer>();
  private LinkedList<io.github.mzmine.datamodel.Scan> parentStack = new LinkedList<>();

  private SimpleFrame buildingFrame;
  private Map<Integer, Double> buildingMobilities;

  public MzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // storage in raw data file
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;
    if (newMZmineFile instanceof IMSRawDataFile) {
      newImsFile = (IMSRawDataFile) newMZmineFile;
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
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

    try {

      MzMLUnmarshaller unmarshaller = new MzMLUnmarshaller(file);

      totalScans = unmarshaller.getObjectCountForXpath("/run/spectrumList/spectrum");

      // fillScanIdTable(
      // unmarshaller.unmarshalCollectionFromXpath("/run/spectrumList/spectrum", Spectrum.class),
      // totalScans);

      MzMLObjectIterator<Spectrum> spectrumIterator = unmarshaller.unmarshalCollectionFromXpath(
          "/run/spectrumList/spectrum", Spectrum.class);

      int mobilityScanNumberCounter = 1;
      Date start = new Date();
      while (spectrumIterator.hasNext()) {

        if (isCanceled()) {
          return;
        }

        Spectrum spectrum = spectrumIterator.next();

        // Ignore scans that are not MS, e.g. UV
        if (!isMsSpectrum(spectrum)) {
          parsedScans++;
          continue;
        }

        String scanId = spectrum.getId();
        // Integer scanNumber = scanIdTable.get(scanId);
        Integer scanNumber = getScanNumber(scanId).orElse(null);
        if (scanNumber == null) {
          throw new IllegalStateException("Cannot determine scan number: " + scanId);
        }

        // Extract scan data
        int msLevel = extractMSLevel(spectrum);
        float retentionTime = (float) extractRetentionTime(spectrum);
        PolarityType polarity = extractPolarity(spectrum);
        // int parentScan = extractParentScanNumber(spectrum);
        double precursorMz = extractPrecursorMz(spectrum);
        Integer precursorCharge = extractPrecursorCharge(spectrum);
        String scanDefinition = extractScanDefinition(spectrum);
        double mzValues[] = extractMzValues(spectrum);
        double intensityValues[] = extractIntensityValues(spectrum);
        Mobility mobility = extractMobility(spectrum);

        // Auto-detect whether this scan is centroided
        MassSpectrumType spectrumType = ScanUtils.detectSpectrumType(mzValues, intensityValues);

        io.github.mzmine.datamodel.Scan scan = null;

        final DDAMsMsInfo info =
            msLevel != 1 && precursorMz != 0d ? new DDAMsMsInfoImpl(precursorMz, precursorCharge,
                null, null, null, msLevel, ActivationMethod.UNKNOWN, null) : null;
        // if (mobility == null) {
        scan = new SimpleScan(newMZmineFile, scanNumber, msLevel, retentionTime, info, mzValues,
            intensityValues, spectrumType, polarity, scanDefinition, null);
        /*
         * } else if (mobility != null && newImsFile != null) { if (buildingFrame == null ||
         * Float.compare(retentionTime, buildingFrame.getRetentionTime()) != 0) {
         * mobilityScanNumberCounter = 1; buildingMobilities = new HashMap<>(); scan =
         * buildingFrame; // for ims, we put the frame into the stack // after all moblity scans
         * have been loaded buildingFrame = new SimpleFrame(newImsFile, scanNumber, msLevel,
         * retentionTime, precursorMz, precursorCharge, mzValues, intensityValues, spectrumType,
         * polarity, scanDefinition, null, mobility.mobilityType(), 0, buildingMobilities, null); }
         * buildingFrame.addMobilityScan( new BuildingMobilityScan(newImsFile,
         * mobilityScanNumberCounter, buildingFrame, mzValues, intensityValues));
         * buildingMobilities.put(mobilityScanNumberCounter, mobility.mobility());
         * mobilityScanNumberCounter++; }
         */

        /*
         * for (io.github.mzmine.datamodel.Scan s : parentStack) { if (s.getScanNumber() ==
         * parentScan) { s.addFragmentScan(scanNumber); } }
         */

        /*
         * Verify the size of parentStack. The actual size of the window to cover possible
         * candidates is defined by limitSize.
         */
        if (parentStack.size() > PARENT_STACK_SIZE && scan != null) {
          io.github.mzmine.datamodel.Scan firstScan = parentStack.removeLast();
          newMZmineFile.addScan(firstScan);
        }
        if (scan != null) {
          parentStack.addFirst(scan);
        }

        parsedScans++;
        if (getFinishedPercentage() >= 5) {
          Date fivePerc = new Date();
          logger.info(
              "Extracted " + parsedScans + " in " + ((fivePerc.getTime() - start.getTime()) / 1000)
                  + " s");
        }

      }

      while (!parentStack.isEmpty()) {
        io.github.mzmine.datamodel.Scan scan = parentStack.removeLast();
        newMZmineFile.addScan(scan);

      }

      if (logger.isLoggable(Level.FINEST)) {
        List<PolarityType> polarities = newMZmineFile.getDataPolarity();
        logger.finest("Scan polarities of file " + file + ": " + polarities);
      }

      project.addFile(newMZmineFile);

    } catch (Throwable e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      e.printStackTrace();
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

  /**
   * Retrieves scan numbers from scan IDs and stores them in scanIdTable.
   * <p>
   * If retrieved scan numbers are not unique, we replace them with new scan numbers.
   *
   * @param iterator iterator from MzMLUnmarshaller
   */
  private void fillScanIdTable(MzMLObjectIterator<Spectrum> iterator, int totalScans) {

    Map<String, Integer> alternativeScanIdTable = new HashMap<>();
    for (int i = 1; iterator.hasNext(); ++i) {
      String id = iterator.next().getId();
      saveScanNumberToTable(id);
      alternativeScanIdTable.put(id, i);
    }

    Set<Integer> scanNumberSet = new HashSet<>(scanIdTable.values());

    if (scanNumberSet.size() != totalScans)
    // Scan Numbers are not unique! We replace them with numbers 1, 2,
    // 3, ...
    {
      scanIdTable = alternativeScanIdTable;
    }
  }

  private void saveScanNumberToTable(String scanId) {

    if (scanIdTable.containsKey(scanId)) {
      return;
    }

    final Matcher matcher = SCAN_PATTERN.matcher(scanId);
    boolean scanNumberFound = matcher.find();

    // Some vendors include scan=XX in the ID, some don't, such as
    // mzML converted from WIFF files. See the definition of nativeID in
    // http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo
    if (scanNumberFound) {
      int scanNumber = Integer.parseInt(matcher.group(1));
      scanIdTable.put(scanId, scanNumber);
      return;
    }

    int scanNumber = lastScanNumber + 1;
    lastScanNumber++;
    scanIdTable.put(scanId, scanNumber);
  }

  private int extractMSLevel(Spectrum spectrum) {
    // Browse the spectrum parameters
    List<CVParam> cvParams = spectrum.getCvParam();
    if (cvParams == null) {
      return 1;
    }
    for (CVParam param : cvParams) {
      String accession = param.getAccession();
      String value = param.getValue();
      if ((accession == null) || (value == null)) {
        continue;
      }

      // MS level MS:1000511
      if (accession.equals("MS:1000511")) {
        int msLevel = Integer.parseInt(value);
        return msLevel;
      }
    }
    return 1;
  }

  private double extractRetentionTime(Spectrum spectrum) {

    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement == null) {
      return 0;
    }
    List<Scan> scanElements = scanListElement.getScan();
    if (scanElements == null) {
      return 0;
    }

    for (Scan scan : scanElements) {
      List<CVParam> cvParams = scan.getCvParam();
      if (cvParams == null) {
        continue;
      }

      for (CVParam param : cvParams) {
        String accession = param.getAccession();
        String unitAccession = param.getUnitAccession();
        String value = param.getValue();
        if ((accession == null) || (value == null)) {
          continue;
        }

        // Retention time (actually "Scan start time") MS:1000016
        if (accession.equals("MS:1000016")) {
          // MS:1000038 is used in mzML 1.0, while UO:0000031
          // is used in mzML 1.1.0 :-/
          double retentionTime;
          if ((unitAccession == null) || (unitAccession.equals("MS:1000038"))
              || unitAccession.equals("UO:0000031")) {
            retentionTime = Double.parseDouble(value);
          } else {
            retentionTime = Double.parseDouble(value) / 60d;
          }
          return retentionTime;

        }
      }
    }

    return 0;
  }

  private double[] extractIntensityValues(Spectrum spectrum) {
    BinaryDataArrayList dataList = spectrum.getBinaryDataArrayList();

    if ((dataList == null) || (dataList.getCount().equals(0))) {
      return new double[0];
    }

    BinaryDataArray intensityArray = dataList.getBinaryDataArray().get(1);
    Number intensityValues[] = intensityArray.getBinaryDataAsNumberArray();
    double dataPoints[] = new double[intensityValues.length];
    for (int i = 0; i < dataPoints.length; i++) {
      double intensity = intensityValues[i].doubleValue();
      dataPoints[i] = intensity;
    }
    return dataPoints;

  }

  private double[] extractMzValues(Spectrum spectrum) {
    BinaryDataArrayList dataList = spectrum.getBinaryDataArrayList();

    if ((dataList == null) || (dataList.getCount().equals(0))) {
      return new double[0];
    }

    BinaryDataArray mzArray = dataList.getBinaryDataArray().get(0);
    Number mzValues[] = mzArray.getBinaryDataAsNumberArray();
    double dataPoints[] = new double[mzValues.length];
    for (int i = 0; i < dataPoints.length; i++) {
      double mz = mzValues[i].doubleValue();
      dataPoints[i] = mz;
    }
    return dataPoints;

  }

  private int extractParentScanNumber(Spectrum spectrum) {
    PrecursorList precursorListElement = spectrum.getPrecursorList();
    if ((precursorListElement == null) || (precursorListElement.getCount().equals(0))) {
      return -1;
    }

    List<Precursor> precursorList = precursorListElement.getPrecursor();
    for (Precursor parent : precursorList) {
      // Get the precursor scan number
      String precursorScanId = parent.getSpectrumRef();
      if (precursorScanId == null) {
        return -1;
      }
      Integer parentScan = scanIdTable.get(precursorScanId);
      if (parentScan == null) {
        return -1;
      }

      return parentScan;
    }
    return -1;
  }

  private double extractPrecursorMz(Spectrum spectrum) {

    PrecursorList precursorListElement = spectrum.getPrecursorList();
    if ((precursorListElement == null) || (precursorListElement.getCount().equals(0))) {
      return 0;
    }

    List<Precursor> precursorList = precursorListElement.getPrecursor();
    for (Precursor parent : precursorList) {

      SelectedIonList selectedIonListElement = parent.getSelectedIonList();
      if ((selectedIonListElement == null) || (selectedIonListElement.getCount().equals(0))) {
        return 0;
      }
      List<ParamGroup> selectedIonParams = selectedIonListElement.getSelectedIon();
      if (selectedIonParams == null) {
        continue;
      }

      for (ParamGroup pg : selectedIonParams) {
        List<CVParam> pgCvParams = pg.getCvParam();
        for (CVParam param : pgCvParams) {
          String accession = param.getAccession();
          String value = param.getValue();
          if ((accession == null) || (value == null)) {
            continue;
          }
          // MS:1000040 is used in mzML 1.0,
          // MS:1000744 is used in mzML 1.1.0
          if (accession.equals("MS:1000040") || accession.equals("MS:1000744")) {
            double precursorMz = Double.parseDouble(value);
            return precursorMz;
          }
        }

      }
    }
    return 0;
  }

  private Integer extractPrecursorCharge(Spectrum spectrum) {
    PrecursorList precursorListElement = spectrum.getPrecursorList();
    if ((precursorListElement == null) || (precursorListElement.getCount().equals(0))) {
      return null;
    }

    List<Precursor> precursorList = precursorListElement.getPrecursor();
    for (Precursor parent : precursorList) {

      SelectedIonList selectedIonListElement = parent.getSelectedIonList();
      if ((selectedIonListElement == null) || (selectedIonListElement.getCount().equals(0))) {
        return null;
      }
      List<ParamGroup> selectedIonParams = selectedIonListElement.getSelectedIon();
      if (selectedIonParams == null) {
        continue;
      }

      for (ParamGroup pg : selectedIonParams) {
        List<CVParam> pgCvParams = pg.getCvParam();
        for (CVParam param : pgCvParams) {
          String accession = param.getAccession();
          String value = param.getValue();
          if ((accession == null) || (value == null)) {
            continue;
          }
          if (accession.equals("MS:1000041")) {
            int precursorCharge = Integer.parseInt(value);
            return precursorCharge;
          }

        }

      }
    }
    return 0;
  }

  private PolarityType extractPolarity(Spectrum spectrum) {
    List<CVParam> cvParams = spectrum.getCvParam();
    if (cvParams != null) {
      for (CVParam param : cvParams) {
        String accession = param.getAccession();

        if (accession == null) {
          continue;
        }
        if (accession.equals("MS:1000130")) {
          return PolarityType.POSITIVE;
        }
        if (accession.equals("MS:1000129")) {
          return PolarityType.NEGATIVE;
        }
      }
    }
    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement != null) {
      List<Scan> scanElements = scanListElement.getScan();
      if (scanElements != null) {
        for (Scan scan : scanElements) {
          cvParams = scan.getCvParam();
          if (cvParams == null) {
            continue;
          }
          for (CVParam param : cvParams) {
            String accession = param.getAccession();
            if (accession == null) {
              continue;
            }
            if (accession.equals("MS:1000130")) {
              return PolarityType.POSITIVE;
            }
            if (accession.equals("MS:1000129")) {
              return PolarityType.NEGATIVE;
            }
          }

        }
      }
    }
    return PolarityType.UNKNOWN;

  }

  private String extractScanDefinition(Spectrum spectrum) {
    List<CVParam> cvParams = spectrum.getCvParam();
    if (cvParams != null) {
      for (CVParam param : cvParams) {
        String accession = param.getAccession();

        if (accession == null) {
          continue;
        }
        if (accession.equals("MS:1000512")) {
          return param.getValue();
        }
      }
    }
    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement != null) {
      List<Scan> scanElements = scanListElement.getScan();
      if (scanElements != null) {
        for (Scan scan : scanElements) {
          cvParams = scan.getCvParam();
          if (cvParams == null) {
            continue;
          }
          for (CVParam param : cvParams) {
            String accession = param.getAccession();
            if (accession == null) {
              continue;
            }
            if (accession.equals("MS:1000512")) {
              return param.getValue();
            }
          }

        }
      }
    }
    return spectrum.getId();
  }

  @Override
  public String getTaskDescription() {
    if (totalScans == 0) {
      return "Opening file " + file;
    } else {
      return "Opening file " + file + ", parsed " + parsedScans + "/" + totalScans + " scans";
    }
  }

  boolean isMsSpectrum(Spectrum spectrum) {

    List<CVParam> cvParams = spectrum.getCvParam();
    if (cvParams != null) {
      for (CVParam param : cvParams) {
        String accession = param.getAccession();
        if (accession == null) {
          continue;
        }

        if (accession.equals("MS:1000804")) {
          return false;
        }
      }
    }

    // By default, let's assume unidentified spectra are MS spectra
    return true;
  }

  /**
   * Agilent/Waters(?) <cvParam cvRef="MS" accession="MS:1002476" name="ion mobility drift time"
   * value= "0.217002108693" unitCvRef="UO" unitAccession="UO:0000028" unitName="millisecond"/>
   * <p>
   * Bruker (converted by Proteowizard MSConvert): <cvParam cvRef="MS" accession="MS:1002815"
   * name="inverse reduced ion mobility" value= "1.582079103978" unitCvRef="MS"
   * unitAccession="MS:1002814" unitName="volt-second per square centimeter"/>
   *
   * @param spectrum
   * @return
   */
  private Mobility extractMobility(Spectrum spectrum) {
    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement == null) {
      return null;
    }
    List<Scan> scanElements = scanListElement.getScan();
    if (scanElements == null) {
      return null;
    }
    for (Scan scan : scanElements) {
      List<CVParam> cvParams = scan.getCvParam();
      if (cvParams == null) {
        continue;
      }
      for (CVParam param : cvParams) {
        String accession = param.getAccession();
        String unitAccession = param.getUnitAccession();
        String value = param.getValue();
        if ((accession == null) || (value == null)) {
          continue;
        }
        // Retention time (actually "Scan start time") MS:1000016
        if (accession.equals("MS:1002476")) {
          // UO:0000028 unitAcession for mobility in Waters files converted to mzML
          double mobility;
          MobilityType mobilityType;
          if ((unitAccession == null) || (unitAccession.equals("UO:0000028"))) {
            mobility = Double.parseDouble(value);
            mobilityType = MobilityType.DRIFT_TUBE;
          } else {
            mobility = Double.parseDouble(value) / 60d;
            mobilityType = MobilityType.NONE;
          }
          return new Mobility(mobility, mobilityType);
        } else if (accession.equals("MS:1002815") && unitAccession.equals("MS:1002814")) {
          return new Mobility(Double.parseDouble(value), MobilityType.TIMS);
        }
      }
    }
    return null;
  }

  public Optional<Integer> getScanNumber(String spectrumId) {
    final Pattern pattern = Pattern.compile("scan=([0-9]+)");
    final Matcher matcher = pattern.matcher(spectrumId);
    boolean scanNumberFound = matcher.find();

    // Some vendors include scan=XX in the ID, some don't, such as
    // mzML converted from WIFF files. See the definition of nativeID in
    // http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo
    // So, get the value of the index tag if the scanNumber is not present in the ID
    if (scanNumberFound) {
      Integer scanNumber = Integer.parseInt(matcher.group(1));
      return Optional.ofNullable(scanNumber);
    }

    final Pattern pattern2 = Pattern.compile("scanId=([0-9]+)");
    final Matcher matcher2 = pattern2.matcher(spectrumId);
    scanNumberFound = matcher2.find();
    if (scanNumberFound) {
      Integer scanNumber = Integer.parseInt(matcher2.group(1));
      return Optional.ofNullable(scanNumber);
    }

    return Optional.ofNullable(null);
  }

  private record Mobility(double mobility, MobilityType mobilityType) {

  }
}
