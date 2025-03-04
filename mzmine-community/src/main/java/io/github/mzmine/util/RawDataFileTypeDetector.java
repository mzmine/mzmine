/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.PolarityType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Detector of raw data file format
 */
public class RawDataFileTypeDetector {

  /*
   * See "https://unidata.ucar.edu/software/netcdf/docs/netcdf_introduction.html#netcdf_format"
   */
  public static final String CDF_HEADER = "CDF";
  public static final String HDF_HEADER = "HDF";
  /*
   * mzML files with index start with <indexedmzML><mzML>tags, but files with no index contain only
   * the <mzML> tag. See
   * "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/schema/mzML1.1.0.xsd"
   */
  public static final String MZML_HEADER = "<mzML";
  /*
   * mzXML files with index start with <mzXML><msRun> tags, but files with no index contain only the
   * <msRun> tag. See "http://sashimi.sourceforge.net/schema_revision/mzXML_3.2/mzXML_3.2.xsd"
   */
  public static final String MZXML_HEADER = "<msRun";
  // See "http://www.psidev.info/sites/default/files/mzdata.xsd.txt"
  public static final String MZDATA_HEADER = "<mzData";
  // See "https://code.google.com/p/unfinnigan/wiki/FileHeader"
  public static final String THERMO_HEADER = String.valueOf(
      new char[]{0x01, 0xA1, 'F', 0, 'i', 0, 'n', 0, 'n', 0, 'i', 0, 'g', 0, 'a', 0, 'n', 0});
  public static final String GZIP_HEADER = String.valueOf(new char[]{0x1f, 0x8b});
  public static final String ZIP_HEADER = String.valueOf(new char[]{'P', 'K', 0x03, 0x04});
  public static final String TDF_SUFFIX = ".tdf";
  public static final String TDF_BIN_SUFFIX = ".tdf_bin";
  public static final String TSF_BIN_SUFFIX = ".tsf_bin";
  public static final String TSF_SUFFIX = ".tsf_bin";
  public static final String BAF_SUFFIX = ".baf";
  public static final String BRUKER_FOLDER_SUFFIX = ".d";
  public static final String AIRD_SUFFIX = ".aird";
  public static final String MZML_SUFFIX = ".mzml";
  public static final String MZXML_SUFFIX = ".mzxml";
  public static final String IMZML_SUFFIX = ".imzml";
  public static final String SCIEX_WIFF_SUFFIX = ".wiff";
  public static final String SCIEX_WIFF2_SUFFIX = ".wiff2";
  public static final String AGILENT_ACQDATATA_FOLDER = "AcqData";

  private static final Logger logger = Logger.getLogger(RawDataFileTypeDetector.class.getName());

  /**
   * @return Detected file type or null if the file is not of any supported type
   */
  public static RawDataFileType detectDataFileType(File fileName) {

    if (fileName.isDirectory()) {
      // To check for Waters .raw directory, we look for _FUNC[0-9]{3}.DAT
      for (File f : fileName.listFiles()) {
        if (f.isFile() && f.getName().toUpperCase().matches("_FUNC[0-9]{3}.DAT")) {
//          DesktopService.getDesktop().displayMessage("Waters raw data detected",
//              "Waters raw data is currently not supported in mzmine. Please use their tool DataConnect to convert zo mzML (see documentation).",
//              "https://mzmine.github.io/mzmine_documentation/data_conversion.html#waters");
//          throw new RuntimeException(
//              "Waters raw data detected. Please download Waters DataConnect(R) and convert to mzML.");
          return detectWatersAcquisitionType(fileName).isIms() ? RawDataFileType.WATERS_RAW_IMS
              : RawDataFileType.WATERS_RAW;
        }
        final var lowerName = f.getName().toLowerCase();
        if (f.isFile() && (lowerName.endsWith(TDF_SUFFIX) || lowerName.endsWith(TDF_BIN_SUFFIX))) {
          return RawDataFileType.BRUKER_TDF;
        }
        if (f.isFile() && (lowerName.endsWith(TSF_SUFFIX) || lowerName.endsWith(TSF_BIN_SUFFIX))) {
          return RawDataFileType.BRUKER_TSF;
        }
        if (f.isFile() && (lowerName.endsWith(BAF_SUFFIX))) {
          return RawDataFileType.BRUKER_BAF;
        }
        if (f.isDirectory() && f.getName().equals(AGILENT_ACQDATATA_FOLDER)) {
          if (new File(f, "IMSFrame.bin").exists()) {
            return RawDataFileType.AGILENT_D_IMS;
          }
          return RawDataFileType.AGILENT_D;
        }
      }
      // We don't recognize any other directory type than Waters and Bruker
      return null;
    }

    try {
      if (fileName.isFile()) {
        var lowerName = fileName.getName().toLowerCase();
        if (lowerName.endsWith(MZML_SUFFIX)) {
          return RawDataFileType.MZML;
        }
        if (lowerName.endsWith(MZXML_SUFFIX)) {
          return RawDataFileType.MZXML;
        }
        if (lowerName.endsWith(IMZML_SUFFIX)) {
          return RawDataFileType.IMZML;
        }
        if (lowerName.endsWith(SCIEX_WIFF_SUFFIX)) {
          return RawDataFileType.SCIEX_WIFF;
        }
        if (lowerName.endsWith(SCIEX_WIFF2_SUFFIX)) {
          return RawDataFileType.SCIEX_WIFF2;
        }
        //the suffix is json and have a .aird file with same name
        /*if (fileName.getName().toLowerCase().endsWith(AIRD_SUFFIX)) {
          String airdIndexFilePath = AirdScanUtil.getIndexPathByAirdPath(fileName.getPath());
          if (airdIndexFilePath != null) {
            File airdIndexFile = new File(airdIndexFilePath);
            if (airdIndexFile.exists()) {
              return RawDataFileType.AIRD;
            }
          }
          logger.info("It's not an aird format file or the aird index file not exist");
        }*/
        if (lowerName.endsWith(TDF_SUFFIX) || lowerName.endsWith(TDF_BIN_SUFFIX)) {
          return RawDataFileType.BRUKER_TDF;
        }
        if (lowerName.endsWith(TSF_SUFFIX) || lowerName.endsWith(TSF_BIN_SUFFIX)) {
          return RawDataFileType.BRUKER_TSF;
        }

        // Read the first 1kB of the file into a String
        String fileHeader = null;
        try (final var reader = Files.newBufferedReader(fileName.toPath(),
            StandardCharsets.ISO_8859_1)) {
          char[] buffer = new char[1024];
          int read = reader.read(buffer);
          if (read > 0) {
            fileHeader = new String(buffer, 0, read);
          }
        }
        if (fileHeader == null) {
          return null;
        }

        if (lowerName.endsWith(".csv")) {
          if (fileHeader.contains(":") && fileHeader.contains("\\") && !fileHeader.contains(
              "file name")) {
            logger.fine("ICP raw file detected");
            return RawDataFileType.ICPMSMS_CSV;
          }
        }

        if (fileHeader.startsWith(THERMO_HEADER)) {
          return RawDataFileType.THERMO_RAW;
        }

        if (fileHeader.startsWith(GZIP_HEADER)) {
          return RawDataFileType.MZML_GZIP;
        }

        if (fileHeader.startsWith(ZIP_HEADER)) {
          return RawDataFileType.MZML_ZIP;
        }

        /*
         * Remove specials (Unicode block) from header if any
         * https://en.wikipedia.org/wiki/Specials_(Unicode_block)
         */
        fileHeader = fileHeader.replaceAll("[^\\x00-\\x7F]", "");

        if (fileHeader.startsWith(CDF_HEADER) || fileHeader.startsWith(HDF_HEADER)) {

          return RawDataFileType.NETCDF;
        }

        if (fileHeader.contains(MZML_HEADER)) {
          return RawDataFileType.MZML;
        }

        if (fileHeader.contains(MZDATA_HEADER)) {
          return RawDataFileType.MZDATA;
        }

        if (fileHeader.contains(MZXML_HEADER)) {
          return RawDataFileType.MZXML;
        }

      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }

    return null;

  }

  /**
   * Currently not used because the import task takes care of everything. Only here as reference
   *
   * @param fileName
   * @return
   * @throws IOException
   */
  @NotNull
  public static RawDataFileType getMzmlType(File fileName) throws IOException {
    if (fileName.getName().toLowerCase().endsWith("imzml")) {
      return RawDataFileType.IMZML;
    } else {
      InputStreamReader reader2 = new InputStreamReader(new FileInputStream(fileName),
          StandardCharsets.ISO_8859_1);
      char[] buffer2 = new char[4096 * 3];
      String content = new String(buffer2);
      boolean containsScan = false, containsAccession = false;
      while (!containsScan && !containsAccession) {
        reader2.read(buffer2);
        content = new String(buffer2);
        content.replaceAll("[^\\x00-\\x7F]", "");
        containsScan = content.contains("/scan");
        containsAccession = (content.contains("1002476") || content.contains("1002815"));
      }
      reader2.close();
      if (content.contains("1002476") || content.contains("1002815")) { // accession for
        // mobility
        return RawDataFileType.MZML_IMS;
      } else {
        return RawDataFileType.MZML;
      }
    }
  }

  @NotNull
  public static WatersAcquisitionInfo detectWatersAcquisitionType(File watersFolder) {
    final Pattern parentFunctionPattern = Pattern.compile(
        "[Ff]unction [Pp]arameters - [Ff]unction [0-9]+ - TOF PARENT FUNCTION");
    final Pattern ddaFunctionPattern = Pattern.compile(
        "[Ff]unction [Pp]arameters - [Ff]unction [0-9]+ - TOF FAST DDA FUNCTION");
    final Pattern surveyFunctionPattern = Pattern.compile(
        "[Ff]unction [Pp]arameters - [Ff]unction [0-9]+ - TOF SURVEY FUNCTION");
    final Pattern referenceFunctionPattern = Pattern.compile(
        "[Ff]unction [Pp]arameters - [Ff]unction [0-9]+ - REFERENCE");
    final Pattern mobilityPattern = Pattern.compile("\\[MOBILITY\\]");
    final Pattern tofModeIMSPattern = Pattern.compile("(TOFMode)(\\s+)(IMS)");
    final Pattern positivePolarityPattern = Pattern.compile(
        "(Polarity)(\\s+)([a-zA-Z]+)?([+]|[Pp]ositive)");
    final Pattern negativePolarityPattern = Pattern.compile(
        "(Polarity)(\\s+)([a-zA-Z]+)?([-]|[Nn]egative)");

    final PatternMatchCounter parentCounter = new PatternMatchCounter(parentFunctionPattern);
    final PatternMatchCounter ddaCounter = new PatternMatchCounter(ddaFunctionPattern);
    final PatternMatchCounter surveyCounter = new PatternMatchCounter(surveyFunctionPattern);
    final PatternMatchCounter referenceCounter = new PatternMatchCounter(referenceFunctionPattern);
    final PatternMatchCounter mobilityCounter = new PatternMatchCounter(mobilityPattern);
    final PatternMatchCounter postiveCounter = new PatternMatchCounter(positivePolarityPattern);
    final PatternMatchCounter negativeCounter = new PatternMatchCounter(negativePolarityPattern);
    final PatternMatchCounter tofModeImsCounter = new PatternMatchCounter(tofModeIMSPattern);


    try (var reader = new BufferedReader(new FileReader(new File(watersFolder, "_extern.inf")))) {
      reader.lines().forEach(line -> {
        parentCounter.checkMatch(line);
        ddaCounter.checkMatch(line);
        surveyCounter.checkMatch(line);
        referenceCounter.checkMatch(line);
        mobilityCounter.checkMatch(line);
        postiveCounter.checkMatch(line);
        negativeCounter.checkMatch(line);
        tofModeImsCounter.checkMatch(line);
      });
      mobilityCounter.matches += tofModeImsCounter.matches;

      final PolarityType polarity =
          (postiveCounter.matches > negativeCounter.matches) ? PolarityType.POSITIVE
              : PolarityType.NEGATIVE;

      if (ddaCounter.getMatches() > 0 && surveyCounter.getMatches() > 0) {
        return new WatersAcquisitionInfo(WatersAcquisitionType.DDA, mobilityCounter.matches > 0,
            polarity);
      }
      if (parentCounter.getMatches() > 1) {
        return new WatersAcquisitionInfo(WatersAcquisitionType.MSE, mobilityCounter.matches > 0,
            polarity);
      } else if (parentCounter.getMatches() == 1) {
        return new WatersAcquisitionInfo(WatersAcquisitionType.MS_ONLY, mobilityCounter.matches > 0,
            polarity);
      }

      logger.info("Unable to detect file type of Waters raw data. Defaulting to MSe and " + (
          mobilityCounter.matches == 0 ? "no " : "") + "mobility separation.");
      return new WatersAcquisitionInfo(WatersAcquisitionType.MSE, mobilityCounter.matches > 0,
          polarity);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public enum WatersAcquisitionType {
    MS_ONLY, DDA, MSE;
  }

  public record WatersAcquisitionInfo(WatersAcquisitionType acquisitionType, boolean isIms,
                                      PolarityType polarity) {

  }


  private static final class PatternMatchCounter {

    private final Pattern pattern;
    private int matches = 0;

    private PatternMatchCounter(Pattern pattern) {
      this.pattern = pattern;
    }

    public Pattern pattern() {
      return pattern;
    }

    public boolean checkMatch(String str) {
      if (pattern.matcher(str).find()) {
        matches++;
        return true;
      }
      return false;
    }

    public int getMatches() {
      return matches;
    }
  }
}
