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

package io.github.mzmine.modules.io.import_rawdata_mzdata;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class read 1.04 and 1.05 MZDATA files.
 */
public class MzDataImportTask extends AbstractTask implements RawDataImportTask {

  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private int totalScans = 0, parsedScans;
  private int peaksCount = 0;
  private StringBuilder charBuffer;
  private boolean precursorFlag = false;
  private boolean spectrumInstrumentFlag = false;
  private boolean mzArrayBinaryFlag = false;
  private boolean intenArrayBinaryFlag = false;
  private String precision, endian;
  private int scanNumber;
  private int msLevel;
  // private int parentScan;
  private PolarityType polarity = PolarityType.UNKNOWN;
  private float retentionTime;
  private double precursorMz;
  private int precursorCharge = 0;
  private DefaultHandler handler = new MzDataHandler();

  /*
   * The information of "m/z" & "int" is content in two arrays because the mzData standard manages
   * this information in two different tags.
   */
  private double[] mzDataPoints;
  private double[] intensityDataPoints;

  /*
   * This variable hold the current scan or fragment, it is send to the stack when another
   * scan/fragment appears as a parser.startElement
   */
  private SimpleScan buildingScan;

  /*
   * This stack stores at most 10 consecutive scans. This window serves to find possible fragments
   * (current scan) that belongs to any of the stored scans in the stack. The reason of the size
   * follows the concept of neighborhood of scans and all his fragments. These solution is
   * implemented because exists the possibility to find fragments of one scan after one or more full
   * scans. The file myo_full_1.05cv.mzdata/myo_full_1.04cv.mzdata, provided by Proteomics Standards
   * Initiative as example, shows this condition in the order of scans and fragments.
   *
   * http://sourceforge.net/projects/psidev/
   */
  private LinkedList<SimpleScan> parentStack;

  public MzDataImportTask(MZmineProject project, File fileToOpen,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.module = module;
    // 256 kilo-chars buffer
    charBuffer = new StringBuilder(1 << 18);
    parentStack = new LinkedList<SimpleScan>();
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = new RawDataFileImpl(fileToOpen.getName(), file.getAbsolutePath(),
        getMemoryMapStorage());
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

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info(
        "Finished parsing " + file + ", parsed " + parsedScans + " of " + totalScans + " scans");
    setStatus(TaskStatus.FINISHED);

  }

  @Override
  public String getTaskDescription() {
    return "Opening file " + file;
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return newMZmineFile;
  }

  private class MzDataHandler extends DefaultHandler {

    @Override
    public void startElement(String namespaceURI, String lName, // local
        // name
        String qName, // qualified name
        Attributes attrs) throws SAXException {

      if (isCanceled()) {
        throw new SAXException("Parsing Cancelled");
      }

      // <spectrumList>
      if (qName.equals("spectrumList")) {
        String s = attrs.getValue("count");
        if (s != null) {
          totalScans = Integer.parseInt(s);
        }
      }

      // <spectrum>
      if (qName.equalsIgnoreCase("spectrum")) {
        msLevel = 0;
        retentionTime = 0f;
        // parentScan = -1;
        polarity = PolarityType.UNKNOWN;
        precursorMz = 0f;
        precursorCharge = 0;
        scanNumber = Integer.parseInt(attrs.getValue("id"));
      }

      // <spectrumInstrument> 1.05 version, <acqInstrument> 1.04 version
      if ((qName.equalsIgnoreCase("spectrumInstrument")) || (qName.equalsIgnoreCase(
          "acqInstrument"))) {
        msLevel = Integer.parseInt(attrs.getValue("msLevel"));
        spectrumInstrumentFlag = true;
      }

      // <cvParam>
      /*
       * The terms time.min, time.sec & mz belongs to mzData 1.04 standard.
       */
      if (qName.equalsIgnoreCase("cvParam")) {
        if (spectrumInstrumentFlag) {
          if ((attrs.getValue("accession").equals("PSI:1000037")) || (attrs.getValue("name")
              .equals("Polarity"))) {
            if (attrs.getValue("value").toLowerCase().equals("positive")) {
              polarity = PolarityType.POSITIVE;
            } else if (attrs.getValue("value").toLowerCase().equals("negative")) {
              polarity = PolarityType.NEGATIVE;
            } else {
              polarity = PolarityType.UNKNOWN;
            }
          }
          if ((attrs.getValue("accession").equals("PSI:1000038")) || (attrs.getValue("name")
              .equals("time.min"))) {
            retentionTime = (float) Double.parseDouble(attrs.getValue("value"));
          }

          if ((attrs.getValue("accession").equals("PSI:1000039")) || (attrs.getValue("name")
              .equals("time.sec"))) {
            retentionTime = (float) (Double.parseDouble(attrs.getValue("value")) / 60d);
          }
        }
        if (precursorFlag) {
          if ((attrs.getValue("accession").equals("PSI:1000040")) || (attrs.getValue("name")
              .equals("mz"))) {
            precursorMz = Double.parseDouble(attrs.getValue("value"));
          }
          if (attrs.getValue("accession").equals("PSI:1000041")) {
            precursorCharge = Integer.parseInt(attrs.getValue("value"));
          }
        }
      }

      // <mzArrayBinary>
      if (qName.equalsIgnoreCase("mzArrayBinary")) {
        // clean the current char buffer for the new element
        mzArrayBinaryFlag = true;
      }

      // <intenArrayBinary>
      if (qName.equalsIgnoreCase("intenArrayBinary")) {
        // clean the current char buffer for the new element
        intenArrayBinaryFlag = true;
      }

      // <data>
      if (qName.equalsIgnoreCase("data")) {
        // clean the current char buffer for the new element
        charBuffer.setLength(0);
        if (mzArrayBinaryFlag) {
          endian = attrs.getValue("endian");
          precision = attrs.getValue("precision");
          String len = attrs.getValue("length");
          if (len != null) {
            peaksCount = Integer.parseInt(len);
          }
        }
        if (intenArrayBinaryFlag) {
          endian = attrs.getValue("endian");
          precision = attrs.getValue("precision");
          String len = attrs.getValue("length");
          if (len != null) {
            peaksCount = Integer.parseInt(len);
          }
        }
      }

      // <precursor>
      if (qName.equalsIgnoreCase("precursor")) {
        /*
         * String parent = attrs.getValue("spectrumRef"); if (parent != null) parentScan =
         * Integer.parseInt(parent); else parentScan = -1;
         */
        precursorFlag = true;
      }
    }

    /**
     * endElement()
     *
     * @throws IOException
     */
    @Override
    public void endElement(String namespaceURI, String sName, // simple name
        String qName // qualified name
    ) throws SAXException {

      // <spectrumInstrument>
      if (qName.equalsIgnoreCase("spectrumInstrument")) {
        spectrumInstrumentFlag = false;
      }

      // <precursor>
      if (qName.equalsIgnoreCase("precursor")) {
        precursorFlag = false;
      }

      // <spectrum>
      if (qName.equalsIgnoreCase("spectrum")) {

        spectrumInstrumentFlag = false;

        // Auto-detect whether this scan is centroided
        MassSpectrumType spectrumType = ScanUtils.detectSpectrumType(mzDataPoints,
            intensityDataPoints);

        final DDAMsMsInfo info =
            msLevel != 1 && Double.compare(precursorMz, 0d) != 0 ? new DDAMsMsInfoImpl(precursorMz,
                precursorCharge, null, null, null, msLevel, ActivationMethod.UNKNOWN, null) : null;

        buildingScan = new SimpleScan(newMZmineFile, scanNumber, msLevel, retentionTime, info,
            mzDataPoints, intensityDataPoints, spectrumType, polarity, "", null);

        /*
         * Verify the size of parentStack. The actual size of the window to cover possible
         * candidates for fragmentScanNumber update is 10 elements.
         */
        if (parentStack.size() > 10) {
          SimpleScan scan = parentStack.removeLast();
          newMZmineFile.addScan(scan);
          parsedScans++;
        }

        parentStack.addFirst(buildingScan);
        buildingScan = null;

      }

      // <mzArrayBinary>
      if (qName.equalsIgnoreCase("mzArrayBinary")) {

        mzArrayBinaryFlag = false;
        mzDataPoints = new double[peaksCount];

        byte[] peakBytes = Base64.getDecoder().decode(charBuffer.toString().trim());

        ByteBuffer currentMzBytes = ByteBuffer.wrap(peakBytes);

        if (endian.equals("big")) {
          currentMzBytes = currentMzBytes.order(ByteOrder.BIG_ENDIAN);
        } else {
          currentMzBytes = currentMzBytes.order(ByteOrder.LITTLE_ENDIAN);
        }

        for (int i = 0; i < mzDataPoints.length; i++) {
          if (precision == null || precision.equals("32")) {
            mzDataPoints[i] = currentMzBytes.getFloat();
          } else {
            mzDataPoints[i] = currentMzBytes.getDouble();
          }
        }
      }

      // <intenArrayBinary>
      if (qName.equalsIgnoreCase("intenArrayBinary")) {

        intenArrayBinaryFlag = false;
        intensityDataPoints = new double[peaksCount];

        byte[] peakBytes = Base64.getDecoder().decode(charBuffer.toString().trim());

        ByteBuffer currentIntensityBytes = ByteBuffer.wrap(peakBytes);

        if (endian.equals("big")) {
          currentIntensityBytes = currentIntensityBytes.order(ByteOrder.BIG_ENDIAN);
        } else {
          currentIntensityBytes = currentIntensityBytes.order(ByteOrder.LITTLE_ENDIAN);
        }

        for (int i = 0; i < intensityDataPoints.length; i++) {
          if (precision == null || precision.equals("32")) {
            intensityDataPoints[i] = currentIntensityBytes.getFloat();
          } else {
            intensityDataPoints[i] = currentIntensityBytes.getDouble();
          }
        }
      }
    }

    /**
     * characters()
     *
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char buf[], int offset, int len) throws SAXException {
      charBuffer.append(buf, offset, len);
    }

    @Override
    public void endDocument() throws SAXException {
      while (!parentStack.isEmpty()) {
        SimpleScan scan = parentStack.removeLast();
        newMZmineFile.addScan(scan);
        parsedScans++;
      }
    }

  }

}
