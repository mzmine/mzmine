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

package io.github.mzmine.modules.io.import_rawdata_mzxml;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CompressionUtils;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class MzXMLImportTask extends AbstractTask {

  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final File file;
  private final MZmineProject project;
  private final RawDataFile newMZmineFile;
  private int totalScans = 0, parsedScans;
  private int peaksCount = 0;
  private final StringBuilder charBuffer;
  private boolean compressFlag = false;
  private final DefaultHandler handler = new MzXMLHandler();
  private String precision;

  // extracted values
  private float retentionTime = 0;
  private int scanNumber = 0;
  private int msLevel = 1;
  private PolarityType polarity = PolarityType.UNKNOWN;
  private String scanId = "";
  private double precursorMz = 0d;
  private int precursorCharge = 0;

  // Retention time parser
  private DatatypeFactory dataTypeFactory;

  /*
   * This variables are used to set the number of fragments that one single scan can have. The
   * initial size of array is set to 10, but it depends of fragmentation level.
   */
  private final int[] parentTreeValue = new int[10];
  private int msLevelTree = 0;

  /*
   * This stack stores the current scan and all his fragments until all the information is recover.
   * The logic is FIFO at the moment of write into the RawDataFile
   */
  private final LinkedList<SimpleScan> parentStack;


  // advanced processing will apply mass detection directly to the scans
  private final boolean applyMassDetection;
  private MZmineProcessingStep<MassDetector> ms1Detector = null;
  private MZmineProcessingStep<MassDetector> ms2Detector = null;

  /*
   * This variable hold the present scan or fragment, it is send to the stack when another
   * scan/fragment appears as a parser.startElement
   */
  private SimpleScan buildingScan;


  public MzXMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    this(project, fileToOpen, newMZmineFile, null, module, parameters, moduleCallDate);
  }

  public MzXMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      AdvancedSpectraImportParameters advancedParam,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // storage in raw data file
    this.parameters = parameters;
    this.module = module;
    // 256 kilo-chars buffer
    charBuffer = new StringBuilder(1 << 18);
    parentStack = new LinkedList<SimpleScan>();
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;

    if (advancedParam != null) {
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
        this.ms1Detector = advancedParam.getParameter(
            AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter().getValue();
      }
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
        this.ms2Detector = advancedParam.getParameter(
            AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter().getValue();
      }
      // currently, we do not support injection times from mzXML file format
//      denormalizeMSnScans = advancedParam.getValue(
//          AdvancedSpectraImportParameters.denormalizeMSnScans);
    }

    this.applyMassDetection = ms1Detector != null || ms2Detector != null;
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

    // Use the default (non-validating) parser
    SAXParserFactory factory = SAXParserFactory.newInstance();

    try {

      dataTypeFactory = DatatypeFactory.newInstance();

      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(file, handler);

      newMZmineFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
      project.addFile(newMZmineFile);

    } catch (Throwable e) {
      e.printStackTrace();
      /* we may already have set the status to CANCELED */
      if (getStatus() == TaskStatus.PROCESSING) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
      }
      return;
    }

    if (isCanceled()) {
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

  @Override
  public String getTaskDescription() {
    return "Opening file " + file;
  }

  private class MzXMLHandler extends DefaultHandler {


    @Override
    public void startElement(String namespaceURI, String lName, // local
        // name
        String qName, // qualified name
        Attributes attrs) throws SAXException {

      if (isCanceled()) {
        throw new SAXException("Parsing Cancelled");
      }

      // <msRun>
      if (qName.equals("msRun")) {
        String s = attrs.getValue("scanCount");
        if (s != null) {
          totalScans = Integer.parseInt(s);
        }
      }

      // <scan>
      if (qName.equalsIgnoreCase("scan")) {

        if (buildingScan != null) {
          parentStack.addFirst(buildingScan);
          buildingScan = null;
        }

        /*
         * Only num, msLevel & peaksCount values are required according with mzxml standard, the
         * others are optional
         */
        scanNumber = Integer.parseInt(attrs.getValue("num"));

        // mzXML files with empty msLevel attribute do exist, so we use
        // 1 as default
        msLevel = 1;
        if (!Strings.isNullOrEmpty(attrs.getValue("msLevel"))) {
          msLevel = Integer.parseInt(attrs.getValue("msLevel"));
        }

        String scanType = attrs.getValue("scanType");
        String filterLine = attrs.getValue("filterLine");
        scanId = filterLine;
        if (Strings.isNullOrEmpty(scanId)) {
          scanId = scanType;
        }

        String polarityAttr = attrs.getValue("polarity");
        if ((polarityAttr != null) && (polarityAttr.length() == 1)) {
          polarity = PolarityType.fromSingleChar(polarityAttr);
        } else {
          polarity = PolarityType.UNKNOWN;
        }
        peaksCount = Integer.parseInt(attrs.getValue("peaksCount"));

        // Parse retention time
        String retentionTimeStr = attrs.getValue("retentionTime");
        if (retentionTimeStr != null) {
          Date currentDate = new Date();
          Duration dur = dataTypeFactory.newDuration(retentionTimeStr);
          retentionTime = (float) (dur.getTimeInMillis(currentDate) / 1000d / 60d);
        } else {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("This file does not contain retentionTime for scans");
          throw new SAXException("Could not read retention time");
        }

        int parentScan = -1;

        if (msLevel > 9) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("msLevel value bigger than 10");
          throw new SAXException("The value of msLevel is bigger than 10");
        }

        /*
         * if (msLevel > 1) { parentScan = parentTreeValue[msLevel - 1]; for (SimpleScan p :
         * parentStack) { if (p.getScanNumber() == parentScan) { p.addFragmentScan(scanNumber); } }
         * }
         */

        // Setting the level of fragment of scan and parent scan number
        msLevelTree++;
        parentTreeValue[msLevel] = scanNumber;
      }

      // <peaks>
      if (qName.equalsIgnoreCase("peaks")) {
        // clean the current char buffer for the new element
        charBuffer.setLength(0);
        compressFlag = false;
        String compressionType = attrs.getValue("compressionType");
        compressFlag = (compressionType != null) && (!compressionType.equals("none"));
        precision = attrs.getValue("precision");

      }

      // <precursorMz>
      if (qName.equalsIgnoreCase("precursorMz")) {
        // clean the current char buffer for the new element
        charBuffer.setLength(0);
        String precursorChargeStr = attrs.getValue("precursorCharge");
        if (precursorChargeStr != null) {
          precursorCharge = Integer.parseInt(precursorChargeStr);
        }
      }

    }

    /**
     * endElement()
     */
    @Override
    public void endElement(String namespaceURI, String sName, // simple name
        String qName // qualified name
    ) throws SAXException {

      // </scan>
      if (qName.equalsIgnoreCase("scan")) {

        msLevelTree--;

        /*
         * At this point we verify if the scan and his fragments are closed, so we include the
         * present scan/fragment into the stack and start to take elements from them (FIFO) for the
         * RawDataFile.
         */

        if (msLevelTree == 0) {
          parentStack.addFirst(buildingScan);
          reset();
          buildingScan = null;
          while (!parentStack.isEmpty()) {
            SimpleScan currentScan = parentStack.removeLast();
            try {
              newMZmineFile.addScan(currentScan);
            } catch (IOException e) {
              e.printStackTrace();
              setStatus(TaskStatus.ERROR);
              setErrorMessage("IO error: " + e);
              throw new SAXException("Parsing error: " + e);
            }
            parsedScans++;
          }

          /*
           * The scan with all his fragments is in the RawDataFile, now we clean the stack for the
           * next scan and fragments.
           */
          parentStack.clear();

        }

        return;
      }

      // <precursorMz>
      if (qName.equalsIgnoreCase("precursorMz")) {
        final String textContent = charBuffer.toString();
        precursorMz = 0d;
        if (!textContent.isEmpty()) {
          precursorMz = Double.parseDouble(textContent);
        }

        return;
      }

      // <peaks>
      if (qName.equalsIgnoreCase("peaks")) {

        byte[] peakBytes = Base64.getDecoder().decode(charBuffer.toString());

        if (compressFlag) {
          try {
            peakBytes = CompressionUtils.decompress(peakBytes);
          } catch (DataFormatException e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Corrupt compressed peak: " + e);
            throw new SAXException("Parsing Cancelled");
          }
        }

        // make a data input stream
        DataInputStream peakStream = new DataInputStream(new ByteArrayInputStream(peakBytes));

        DataPoint[] dps = new DataPoint[peaksCount];
        double[] mzValues = new double[peaksCount];
        double[] intensityValues = new double[peaksCount];

        try {
          for (int i = 0; i < peaksCount; i++) {

            // Always respect this order pairOrder="m/z-int"
            double mz;
            double intensity;
            if ("64".equals(precision)) {
              mz = peakStream.readDouble();
              intensity = peakStream.readDouble();
            } else {
              mz = peakStream.readFloat();
              intensity = peakStream.readFloat();
            }

            // Copy m/z and intensity data
            dps[i] = new SimpleDataPoint(mz, intensity);
          }
          // sort because old converters might create unsorted spectral data
          Arrays.sort(dps, DataPointSorter.DEFAULT_MZ_ASCENDING);

          for (int i = 0; i < dps.length; i++) {
            mzValues[i] = dps[i].getMZ();
            intensityValues[i] = dps[i].getIntensity();
          }

        } catch (IOException eof) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Corrupt mzXML file");
          throw new SAXException("Parsing Cancelled");
        }

        if (applyMassDetection) {
          // wrap scan
          double[][] mzIntensities = null;

          // apply mass detection
          if (ms1Detector != null && msLevel == 1) {
            mzIntensities = applyMassDetection(ms1Detector, mzValues, intensityValues);
          } else if (ms2Detector != null && msLevel >= 2) {
            mzIntensities = applyMassDetection(ms2Detector, mzValues, intensityValues);
          }

          if (mzIntensities != null) {
            final DDAMsMsInfo info =
                msLevel != 1 && Double.compare(precursorMz, 0d) != 0 ? new DDAMsMsInfoImpl(
                    precursorMz, precursorCharge, null, null, null, msLevel,
                    ActivationMethod.UNKNOWN, null) : null;
            // Set the centroided / thresholded data points to the scan
            buildingScan = new SimpleScan(newMZmineFile, scanNumber, msLevel, retentionTime, info,
                mzIntensities[0], mzIntensities[1], MassSpectrumType.CENTROIDED, polarity, scanId,
                null);

            // create mass list and scan. Override data points and spectrum type
            ScanPointerMassList newMassList = new ScanPointerMassList(buildingScan);
            buildingScan.addMassList(newMassList);
          }
        }

        // if no mass dection was applied - just create the scan
        if (buildingScan == null) {
          // Auto-detect whether this scan is centroided
          MassSpectrumType spectrumType = ScanUtils.detectSpectrumType(mzValues, intensityValues);

          final DDAMsMsInfo info =
              msLevel != 1 && precursorMz != 0d ? new DDAMsMsInfoImpl(precursorMz, precursorCharge,
                  null, null, null, msLevel, ActivationMethod.UNKNOWN, null) : null;

          // Set the final data points to the scan
          buildingScan = new SimpleScan(newMZmineFile, scanNumber, msLevel, retentionTime, info,
              mzValues, intensityValues, spectrumType, polarity, scanId, null);
        }

      }
    }

    /**
     * Apply mass detection
     *
     * @param msDetector  mass detection module
     * @param mzs         input values for mass detection
     * @param intensities input values
     * @return new mzs: double[0]; new intensities: double[1] arrays
     */
    private double[][] applyMassDetection(MZmineProcessingStep<MassDetector> msDetector,
        double[] mzs, double[] intensities) {
      // wrap data points in a simple mass spectrum
      return msDetector.getModule()
          .getMassValues(new SimpleMassSpectrum(mzs, intensities), msDetector.getParameterSet());
    }

    private void reset() {
      buildingScan = null;
      retentionTime = 0;
      scanNumber = 0;
      msLevel = 1;
      polarity = PolarityType.UNKNOWN;
      scanId = "";
      precursorMz = 0d;
      precursorCharge = 0;
    }

    /**
     * characters()
     *
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] buf, int offset, int len) throws SAXException {
      charBuffer.append(buf, offset, len);
    }
  }

}
