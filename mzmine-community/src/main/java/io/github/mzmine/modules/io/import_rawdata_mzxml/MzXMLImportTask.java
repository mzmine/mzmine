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

package io.github.mzmine.modules.io.import_rawdata_mzxml;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CompressionUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class MzXMLImportTask extends AbstractTask implements RawDataImportTask {

  private final ScanImportProcessorConfig scanProcessorConfig;
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

  // Retention time parser
  private DatatypeFactory dataTypeFactory;

  /*
   * This stack stores the current scan and all his fragments until all the information is recover.
   * The logic is FIFO at the moment of write into the RawDataFile
   */
  private final List<SimpleScan> parentStack = new ArrayList<>();

  /**
   * MS1 scan opens then may have MS2 then MS3 and closing tags are only after the top N scans are
   * done
   */
  private int numOpenScans = 0;

  /*
   * This variable hold the present scan or fragment, it is send to the stack when another
   * scan/fragment appears as a parser.startElement
   */
  private SimpleBuildingScan buildingScan;
  private SimpleScan lastScan;


  public MzXMLImportTask(MZmineProject project, File fileToOpen,
      @NotNull ScanImportProcessorConfig scanProcessorConfig,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, final @Nullable MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage also set in raw data file
    this.scanProcessorConfig = scanProcessorConfig;
    this.parameters = parameters;
    this.module = module;
    // 256 kilo-chars buffer
    charBuffer = new StringBuilder(1 << 18);
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = new RawDataFileImpl(file.getName(), file.getAbsolutePath(),
        getMemoryMapStorage());
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

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
      logger.log(Level.WARNING, "During loading of data file. " + e.getMessage(), e);
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

  private void processAndFinalizeBuildingScan(SimpleSpectralArrays data) {
    // Auto-detect whether this scan is centroided
    if (buildingScan.spectrumType == null) {
      buildingScan.spectrumType = ScanUtils.detectSpectrumType(data.mzs(), data.intensities());
    }

    // data reading finished, apply data processing like sorting cropping mass detection if selected
    var processedData = scanProcessorConfig.processor().processScan(buildingScan, data);
    double[] mzs = processedData.mzs();
    double[] intensities = processedData.intensities();

    // Change spectrum type
    if (scanProcessorConfig.isMassDetectActive(buildingScan.msLevel)) {
      buildingScan.spectrumType = MassSpectrumType.CENTROIDED;
    }

    lastScan = new SimpleScan(newMZmineFile, buildingScan.scanNumber, buildingScan.msLevel,
        buildingScan.retentionTime, buildingScan.getMsMsInfo(), mzs, intensities,
        buildingScan.spectrumType, buildingScan.polarity, buildingScan.scanId, null);

    if (scanProcessorConfig.isMassDetectActive(buildingScan.msLevel)) {
      // create mass list and scan. Override data points and spectrum type
      lastScan.addMassList(new ScanPointerMassList(lastScan));
    }

    // after peaks the scan is done - add to stack and wait for last scan to be closed before adding to file
    parentStack.add(lastScan);
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? newMZmineFile : null;
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
        numOpenScans++;

        lastScan = null;
        buildingScan = new SimpleBuildingScan();

        /*
         * Only num, msLevel & peaksCount values are required according with mzxml standard, the
         * others are optional
         */
        buildingScan.scanNumber = Integer.parseInt(attrs.getValue("num"));

        // mzXML files with empty msLevel attribute do exist, so we use
        // 1 as default
        buildingScan.msLevel = 1;
        if (!Strings.isNullOrEmpty(attrs.getValue("msLevel"))) {
          buildingScan.msLevel = Integer.parseInt(attrs.getValue("msLevel"));
        }

        String scanType = attrs.getValue("scanType");
        String filterLine = attrs.getValue("filterLine");
        buildingScan.scanId = filterLine;
        if (Strings.isNullOrEmpty(buildingScan.scanId)) {
          buildingScan.scanId = scanType;
        }

        String polarityAttr = attrs.getValue("polarity");
        if ((polarityAttr != null) && (polarityAttr.length() == 1)) {
          buildingScan.polarity = PolarityType.fromSingleChar(polarityAttr);
        } else {
          buildingScan.polarity = PolarityType.UNKNOWN;
        }
        peaksCount = Integer.parseInt(attrs.getValue("peaksCount"));

        // Parse retention time
        String retentionTimeStr = attrs.getValue("retentionTime");
        if (retentionTimeStr != null) {
          Date currentDate = new Date();
          Duration dur = dataTypeFactory.newDuration(retentionTimeStr);
          buildingScan.retentionTime = (float) (dur.getTimeInMillis(currentDate) / 1000d / 60d);
        } else {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("This file does not contain retentionTime for scans");
          throw new SAXException("Could not read retention time");
        }

        if (buildingScan.msLevel > 9) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("msLevel value bigger than 10");
          throw new SAXException("The value of msLevel is bigger than 10");
        }

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
          buildingScan.precursorCharge = Integer.parseInt(precursorChargeStr);
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
        numOpenScans--;
        if (numOpenScans < 0) {
          numOpenScans = 0;
        }
        if (numOpenScans == 0) {
          for (final SimpleScan scan : parentStack) {
            newMZmineFile.addScan(scan);
          }
          parentStack.clear();
        }
        reset();
        parsedScans++;
        return;
      }

      // <precursorMz>
      if (qName.equalsIgnoreCase("precursorMz")) {
        final String textContent = charBuffer.toString();
        buildingScan.precursorMz = 0d;
        if (!textContent.isEmpty()) {
          buildingScan.precursorMz = Double.parseDouble(textContent);
          if (buildingScan.precursorMz > 0 && buildingScan.msLevel <= 0) {
            buildingScan.msLevel = 2;
          }
        }

        return;
      }

      // <peaks>
      if (qName.equalsIgnoreCase("peaks")) {
        // this is the last element
        // only read and process data if needed (scan matches filters)

        if (scanProcessorConfig.scanFilter().matches(buildingScan)) {
          SimpleSpectralArrays data = readSpectralData();
          processAndFinalizeBuildingScan(data);
        }
      }
    }

    @NotNull
    private SimpleSpectralArrays readSpectralData() throws SAXException {
      SimpleSpectralArrays data;
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
          mzValues[i] = mz;
          intensityValues[i] = intensity;
        }
        data = new SimpleSpectralArrays(mzValues, intensityValues);
      } catch (IOException eof) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Corrupt mzXML file");
        throw new SAXException("Parsing Cancelled");
      }
      return data;
    }

    private void reset() {
      lastScan = null;
      buildingScan = null;
    }

    /**
     * characters()
     *
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] buf, int offset, int len) {
      charBuffer.append(buf, offset, len);
    }
  }

}
