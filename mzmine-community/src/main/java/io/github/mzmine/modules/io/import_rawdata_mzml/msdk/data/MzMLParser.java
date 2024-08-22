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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import io.github.msdk.datamodel.Chromatogram;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.TagTracker;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * Used to parse mzML meta-data and initialize {@link MzMLBinaryDataInfo MzMLBinaryDataInfo}
 * </p>
 */
public class MzMLParser {

  private static final Logger logger = Logger.getLogger(MzMLParser.class.getName());

  private final Vars vars;
  private final TagTracker tracker;
  private final MemoryMapStorage storage;
  private final @NotNull ScanImportProcessorConfig scanProcessorConfig;
  private final File mzMLFile;

  private final MzMLRawDataFile newRawFile;
  private final Pattern scanNumberPattern = Pattern.compile("scan=([0-9]+)");
  private final Pattern agilentScanNumberPattern = Pattern.compile("scan[iI]d=([0-9]+)");
  private final Map<String, MzMLCompressionType> compressionTypeMap = Arrays.stream(
          MzMLCompressionType.values())
      .collect(Collectors.toMap(MzMLCompressionType::getAccession, Function.identity()));
  private final Map<String, MzMLBitLength> bitlengthMap = Arrays.stream(MzMLBitLength.values())
      .collect(Collectors.toMap(MzMLBitLength::getAccession, Function.identity()));
  private final Map<String, MzMLArrayType> arrayTypeMap = Arrays.stream(MzMLArrayType.values())
      .collect(Collectors.toMap(MzMLArrayType::getAccession, Function.identity()));
  private int totalScans = 0, parsedScans = 0;

  public MzMLParser(MSDKmzMLImportTask importer, MemoryMapStorage storage,
      @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    this.vars = new Vars();
    this.tracker = new TagTracker();
    mzMLFile = importer.getMzMLFile();
    this.newRawFile = new MzMLRawDataFile(mzMLFile, vars.msFunctionsList, vars.chromatogramsList,
        vars.mobilityScanData);
    this.storage = storage;
    this.scanProcessorConfig = scanProcessorConfig;
  }

  /**
   * <p>
   * Carry out the required parsing of the mzML data when the
   * {@link XMLStreamReader XMLStreamReader} enters the given tag
   * </p>
   *
   * @param xmlStreamReader an instance of {@link XMLStreamReader XMLStreamReader
   * @param is              {@link InputStream InputStream} of the mzML data
   * @param openingTagName  The tag <code>xmlStreamReader</code> entered
   */
  public void processOpeningTag(XMLStreamReader xmlStreamReader, String openingTagName)
      throws IOException, DataFormatException, XMLStreamException {
    tracker.enter(openingTagName);

    if (tracker.current().contentEquals((MzMLTags.TAG_RUN))) {
      final String defaultInstrumentConfigurationRef = getRequiredAttribute(xmlStreamReader,
          MzMLTags.ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF);
      newRawFile.setDefaultInstrumentConfiguration(defaultInstrumentConfigurationRef);

      // startTimeStamp may be optional, so it makes no sense to stop import of a RawDataFile
      // if this tag is skipped
      final String startTimeStamp = xmlStreamReader.getAttributeValue(null,
          MzMLTags.ATTR_START_TIME_STAMP);
      if (startTimeStamp != null) {
        newRawFile.setStartTimeStamp(startTimeStamp);
        logger.info("startTimeStamp value is: " + startTimeStamp);
      } else {
        logger.info("startTimeStamp wasn't set");
      }
    }

    if (tracker.current().contentEquals((MzMLTags.TAG_SPECTRUM_LIST))) {
      final String defaultDataProcessingRefScan = getRequiredAttribute(xmlStreamReader,
          MzMLTags.ATTR_DEFAULT_DATA_PROCESSING_REF);
      newRawFile.setDefaultDataProcessingScan(defaultDataProcessingRefScan);
    }

//    if (tracker.current().contentEquals((MzMLTags.TAG_CHROMATOGRAM_LIST))) {
//      final String defaultDataProcessingRefChromatogram = getRequiredAttribute(xmlStreamReader,
//          MzMLTags.ATTR_DEFAULT_DATA_PROCESSING_REF);
//      newRawFile.setDefaultDataProcessingChromatogram(
//          defaultDataProcessingRefChromatogram.toString());
//    }

    if (openingTagName.contentEquals((MzMLTags.TAG_SPECTRUM_LIST))) {
      final String count = getRequiredAttribute(xmlStreamReader, "count");
      this.totalScans = Integer.parseInt(count);
    }

    if (tracker.inside(MzMLTags.TAG_REF_PARAM_GROUP_LIST)) {

      if (openingTagName.contentEquals(MzMLTags.TAG_REF_PARAM_GROUP)) {
        final String id = getRequiredAttribute(xmlStreamReader, "id");
        vars.referenceableParamGroup = new MzMLReferenceableParamGroup(id);

      } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
        MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
        vars.referenceableParamGroup.addCVParam(cvParam);
      }
    }

    if (tracker.inside(MzMLTags.TAG_SPECTRUM_LIST)) {
      if (openingTagName.contentEquals(MzMLTags.TAG_SPECTRUM)) {
        String id = getRequiredAttribute(xmlStreamReader, "id");
        int index = Integer.parseInt(getRequiredAttribute(xmlStreamReader, "index"));
        vars.defaultArrayLength = Integer.parseInt(
            getRequiredAttribute(xmlStreamReader, "defaultArrayLength"));
        Integer scanNumber = getScanNumber(id).orElse(index + 1);
        //        vars.spectrum = new BuildingMzMLMsScan(newRawFile, id, scanNumber, vars.defaultArrayLength);
        vars.spectrum = new BuildingMzMLMsScan(id, scanNumber, vars.defaultArrayLength);
      } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY_DATA_ARRAY)) {
        vars.skipBinaryDataArray = false;
        int encodedLength = Integer.parseInt(
            getRequiredAttribute(xmlStreamReader, "encodedLength"));
        final String arrayLength = xmlStreamReader.getAttributeValue(null, "arrayLength");
        if (arrayLength != null) {
          vars.binaryDataInfo = new MzMLBinaryDataInfo(encodedLength,
              Integer.parseInt(arrayLength));
        } else {
          vars.binaryDataInfo = new MzMLBinaryDataInfo(encodedLength, vars.defaultArrayLength);
        }

      } else if (openingTagName.contentEquals(MzMLTags.TAG_SCAN)) {
        vars.scan = new MzMLScan();

      } else if (openingTagName.contentEquals(MzMLTags.TAG_SCAN_WINDOW_LIST)) {
        vars.scanWindowList = new MzMLScanWindowList();

      } else if (openingTagName.contentEquals(MzMLTags.TAG_SCAN_WINDOW)) {
        vars.scanWindow = new MzMLScanWindow();

      } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
        if (!tracker.inside(MzMLTags.TAG_BINARY_DATA_ARRAY_LIST) && !tracker.inside(
            MzMLTags.TAG_PRODUCT_LIST) && !tracker.inside(MzMLTags.TAG_PRECURSOR_LIST)
            && !tracker.inside(MzMLTags.TAG_SCAN_LIST) && vars.spectrum != null) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);

          // Some vendors report a different TIC CV param value from the one obtained
          // from integrating from the scan's intensities.
          // Retain the MzMLCV.cvTIC so it can be retrieved from the MzMLMsScan object.
          // if (!cvParam.getAccession().equals(MzMLCV.cvTIC))
          vars.spectrum.getCVParams().addCVParam(cvParam);

        } else if (tracker.inside(MzMLTags.TAG_SCAN_LIST)) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
          if (!tracker.inside(MzMLTags.TAG_SCAN_WINDOW)) {
            if (!tracker.inside(MzMLTags.TAG_SCAN)) {
              vars.spectrum.getScanList().addCVParam(cvParam);
            } else {
              vars.scan.addCVParam(cvParam);
            }
          } else {
            vars.scanWindow.addCVParam(cvParam);
          }

        } else if (tracker.inside(MzMLTags.TAG_SPECTRUM) && tracker.inside(
            MzMLTags.TAG_BINARY_DATA_ARRAY) && !vars.skipBinaryDataArray) {
          String accession = getRequiredAttribute(xmlStreamReader, "accession");
          if (bitlengthMap.get(accession) != null) {
            vars.binaryDataInfo.setBitLength(bitlengthMap.get(accession));
          } else if (compressionTypeMap.get(accession) != null) {
            manageCompression(vars.binaryDataInfo, accession);
          } else if (arrayTypeMap.get(accession) != null) {
            vars.binaryDataInfo.setArrayType(arrayTypeMap.get(accession));
          } else {
            vars.skipBinaryDataArray = true;
          }
        }

      } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY)) {
        //todo check if we can put this before previous if and use this to create boolean to indicate detection of both mzs and intensities
        if (!vars.skipBinaryDataArray) {
          if (MzMLArrayType.MZ == vars.binaryDataInfo.getArrayType()) {
            vars.spectrum.setMzBinaryDataInfo(vars.binaryDataInfo);
          }
          if (MzMLArrayType.INTENSITY == vars.binaryDataInfo.getArrayType()) {
            vars.spectrum.setIntensityBinaryDataInfo(vars.binaryDataInfo);
          }
          if (MzMLArrayType.WAVELENGTH == vars.binaryDataInfo.getArrayType()) {
            vars.spectrum.setWavelengthBinaryDataInfo(vars.binaryDataInfo);
          }
        }
        if (vars.spectrum != null && !vars.skipBinaryDataArray) {
          //here we obtain the text value of the whole TAG_BINARY
          //using getElementText() requires exiting the tracker afterwards, otherwise xmlStreamReader produces an error
          var binaryContent = xmlStreamReader.getElementText();
          vars.binaryDataInfo.setTextContent(binaryContent);
          tracker.exit(tracker.current());
        }
      } else if (openingTagName.contentEquals(MzMLTags.TAG_REF_PARAM_GROUP_REF)) {
        String refValue = getRequiredAttribute(xmlStreamReader, "ref");
        for (MzMLReferenceableParamGroup ref : vars.referenceableParamGroupList) {
          if (ref.getParamGroupName().equals(refValue)) {
            vars.spectrum.getCVParams().getCVParamsList().addAll(ref.getCVParamsList());
            break;
          }
        }

      } else if (openingTagName.contentEquals(MzMLTags.TAG_PRODUCT)) {
        vars.product = new MzMLProduct();
      }

      if (tracker.inside(MzMLTags.TAG_PRECURSOR_LIST)) {

        if (openingTagName.contentEquals(MzMLTags.TAG_PRECURSOR)) {
          final String spectrumRef = xmlStreamReader.getAttributeValue(null,
              MzMLTags.ATTR_SPECTRUM_REF);
          vars.precursor = new MzMLPrecursorElement(spectrumRef);

        } else if (openingTagName.contentEquals(MzMLTags.TAG_ISOLATION_WINDOW)) {
          vars.isolationWindow = new MzMLIsolationWindow();

        } else if (openingTagName.contentEquals(MzMLTags.TAG_SELECTED_ION_LIST)) {
          vars.selectedIonList = new MzMLPrecursorSelectedIonList();

        } else if (openingTagName.contentEquals(MzMLTags.TAG_ACTIVATION)) {
          vars.activation = new MzMLPrecursorActivation();

        } else if (tracker.inside(MzMLTags.TAG_ISOLATION_WINDOW)) {
          if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
            MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
            vars.isolationWindow.addCVParam(cvParam);
          }
          if (openingTagName.contentEquals(MzMLTags.TAG_USER_PARAM)) {
            //            <userParam name="ms level" value="1"/>
            // user params are optional - MS convert 3.0 21341  uses this format for MSn
            final MzMLUserParam userParam = createMzMLUserParam(xmlStreamReader);
            if (userParam != null && MzMLUserParam.MS_LEVEL_IN_PRECURSOR_LIST.equals(
                userParam.name())) {
              vars.isolationWindow.setMSLevel(userParam.value());
            }
          }

        } else if (tracker.inside(MzMLTags.TAG_SELECTED_ION_LIST)) {
          if (openingTagName.contentEquals(MzMLTags.TAG_SELECTED_ION)) {
            vars.selectedIon = new MzMLPrecursorSelectedIon();
          } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
            MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
            vars.selectedIon.addCVParam(cvParam);
          }

        } else if (tracker.inside(MzMLTags.TAG_ACTIVATION)) {
          if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
            MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
            vars.activation.addCVParam(cvParam);
          }
        }
      }

      if (tracker.inside(MzMLTags.TAG_PRODUCT_LIST)) {
        if (openingTagName.contentEquals(MzMLTags.TAG_ISOLATION_WINDOW)) {
          vars.isolationWindow = new MzMLIsolationWindow();

        } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
          vars.isolationWindow.addCVParam(cvParam);
        }

      }
    } else if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM_LIST)) {
      parseTagInsideChromatogramList(xmlStreamReader, openingTagName);
    }
  }

  private void parseTagInsideChromatogramList(XMLStreamReader xmlStreamReader,
      String openingTagName) throws XMLStreamException {
    if (openingTagName.contentEquals(MzMLTags.TAG_CHROMATOGRAM)) {
      String chromatogramId = getRequiredAttribute(xmlStreamReader, "id").toString();
      Integer chromatogramNumber =
          Integer.parseInt(getRequiredAttribute(xmlStreamReader, "index")) + 1;
      vars.defaultArrayLength = Integer.parseInt(
          getRequiredAttribute(xmlStreamReader, "defaultArrayLength"));
      vars.chromatogram = new MzMLChromatogram(newRawFile, chromatogramId, chromatogramNumber,
          vars.defaultArrayLength);
    } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
      if (!tracker.inside(MzMLTags.TAG_BINARY_DATA_ARRAY) && !tracker.inside(MzMLTags.TAG_PRECURSOR)
          && !tracker.inside(MzMLTags.TAG_PRODUCT) && vars.chromatogram != null) {
        MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
        vars.chromatogram.getCVParams().addCVParam(cvParam);
      }
    } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY_DATA_ARRAY)) {
      vars.skipBinaryDataArray = false;
      int encodedLength = Integer.parseInt(getRequiredAttribute(xmlStreamReader, "encodedLength"));
      final String arrayLength = xmlStreamReader.getAttributeValue(null, "arrayLength");
      if (arrayLength != null) {
        vars.binaryDataInfo = new MzMLBinaryDataInfo(encodedLength, Integer.parseInt(arrayLength));
      } else {
        vars.binaryDataInfo = new MzMLBinaryDataInfo(encodedLength, vars.defaultArrayLength);
      }
    } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY)) {
      if (vars.chromatogram != null && !vars.skipBinaryDataArray) {
        vars.chromatogram.processBinaryChromatogramValues(xmlStreamReader.getElementText(),
            vars.binaryDataInfo);
        tracker.exit(tracker.current());
      }

    } else if (openingTagName.contentEquals(MzMLTags.TAG_REF_PARAM_GROUP_REF)) {
      String refValue = xmlStreamReader.getAttributeValue(null, "ref").toString();
      for (MzMLReferenceableParamGroup ref : vars.referenceableParamGroupList) {
        if (ref.getParamGroupName().equals(refValue)) {
          vars.chromatogram.getCVParams().getCVParamsList().addAll(ref.getCVParamsList());
          break;
        }
      }
    }

    if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM) && tracker.inside(MzMLTags.TAG_BINARY_DATA_ARRAY)
        && openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM) && vars.binaryDataInfo != null
        && !vars.skipBinaryDataArray) {
      String accession = getRequiredAttribute(xmlStreamReader, "accession").toString();
      if (vars.binaryDataInfo.isBitLengthAccession(accession)) {
        vars.binaryDataInfo.setBitLength(MzMLBitLength.of(accession));
      } else if (MzMLCompressionType.isCompressionTypeAccession(accession)) {
        manageCompression(vars.binaryDataInfo, accession);
      } else if (MzMLArrayType.isArrayTypeAccession(accession)) {
        vars.binaryDataInfo.setArrayType(MzMLArrayType.ofAccession(accession));
        final String unitAccession = getRequiredAttribute(xmlStreamReader, "unitAccession");
        vars.binaryDataInfo.setUnitAccession(unitAccession);

        if (MzMLCV.cvRetentionTimeArray.equals(vars.binaryDataInfo.getArrayType().getAccession())) {
          vars.chromatogram.setRtBinaryDataInfo(vars.binaryDataInfo);
        }
        if (MzMLCV.cvIntensityArray.equals(vars.binaryDataInfo.getArrayType().getAccession())) {
          vars.chromatogram.setIntensityBinaryDataInfo(vars.binaryDataInfo);
        }
      } else {
        vars.skipBinaryDataArray = true;
      }

    }

    if (openingTagName.contentEquals(MzMLTags.TAG_PRECURSOR)) {
      final String spectrumRef = xmlStreamReader.getAttributeValue(null, "spectrumRef");
      String spectrumRefString = spectrumRef == null ? null : spectrumRef.toString();
      vars.precursor = new MzMLPrecursorElement(spectrumRefString);

    } else if (openingTagName.contentEquals(MzMLTags.TAG_PRODUCT)) {
      vars.product = new MzMLProduct();

    } else if (tracker.inside(MzMLTags.TAG_PRECURSOR)) {
      if (openingTagName.contentEquals(MzMLTags.TAG_ISOLATION_WINDOW)) {
        vars.isolationWindow = new MzMLIsolationWindow();
        vars.selectedIonList = new MzMLPrecursorSelectedIonList();

      } else if (openingTagName.contentEquals(MzMLTags.TAG_ACTIVATION)) {
        vars.activation = new MzMLPrecursorActivation();

      } else if (tracker.inside(MzMLTags.TAG_ISOLATION_WINDOW)) {
        if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
          vars.isolationWindow.addCVParam(cvParam);
        }

      } else if (tracker.inside(MzMLTags.TAG_SELECTED_ION_LIST)) {
        if (openingTagName.contentEquals(MzMLTags.TAG_SELECTED_ION)) {
          vars.selectedIon = new MzMLPrecursorSelectedIon();
        } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
          vars.selectedIon.addCVParam(cvParam);
        }

      } else if (tracker.inside(MzMLTags.TAG_ACTIVATION)) {
        if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
          vars.activation.addCVParam(cvParam);
        }
      }
    } else if (tracker.inside(MzMLTags.TAG_PRODUCT)) {
      if (openingTagName.contentEquals(MzMLTags.TAG_ISOLATION_WINDOW)) {
        vars.isolationWindow = new MzMLIsolationWindow();

      } else if (tracker.inside(MzMLTags.TAG_ISOLATION_WINDOW)) {
        if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
          vars.isolationWindow.addCVParam(cvParam);

        }

      }
    }
  }

  private MzMLUserParam createMzMLUserParam(XMLStreamReader xmlStreamReader) {
    String name = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_NAME);
    String value = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_VALUE);
    if (name != null && value != null) {
      return new MzMLUserParam(name, value);
    }
    return null;
  }

  /**
   * <p>
   * Carry out the required parsing of the mzML data when the
   * {@link XMLStreamReader XMLStreamReader} exits the given tag
   * </p>
   *
   * @param xmlStreamReader an instance of {@link XMLStreamReader XMLStreamReader
   * @param closingTagName  a {@link String} object.
   */
  public void processClosingTag(XMLStreamReader xmlStreamReader, String closingTagName) {
    tracker.exit(closingTagName);

    if (closingTagName.equals(MzMLTags.TAG_SPECTRUM)) {
      this.parsedScans++;
    }

    if (closingTagName.equals(MzMLTags.TAG_REF_PARAM_GROUP)) {
      vars.referenceableParamGroupList.add(vars.referenceableParamGroup);

    } else if (closingTagName.equals(MzMLTags.TAG_ISOLATION_WINDOW)) {
      if (tracker.inside(MzMLTags.TAG_PRECURSOR)) {
        vars.precursor.setIsolationWindow(vars.isolationWindow);
      } else if (tracker.inside(MzMLTags.TAG_PRODUCT)) {
        vars.product.setIsolationWindow(vars.isolationWindow);
      }

    } else if (closingTagName.equals(MzMLTags.TAG_PRODUCT)) {
      if (tracker.inside(MzMLTags.TAG_SPECTRUM)) {
        vars.spectrum.getProductList().addProduct(vars.product);
      } else if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM)) {
        vars.chromatogram.setProdcut(vars.product);
      }
    } else if (closingTagName.equals(MzMLTags.TAG_SELECTED_ION_LIST)) {
      vars.precursor.setSelectedIonList(vars.selectedIonList);

    } else if (closingTagName.equals(MzMLTags.TAG_ACTIVATION)) {
      vars.precursor.setActivation(vars.activation);

    } else if (closingTagName.equals(MzMLTags.TAG_SELECTED_ION)) {
      vars.selectedIonList.addSelectedIon(vars.selectedIon);

    } else if (closingTagName.equals(MzMLTags.TAG_PRECURSOR)) {
      if (tracker.inside(MzMLTags.TAG_SPECTRUM)) {
        vars.spectrum.getPrecursorList().addPrecursor(vars.precursor);
      } else if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM)) {
        vars.chromatogram.setPrecursor(vars.precursor);
      }

    } else if (closingTagName.equals(MzMLTags.TAG_SCAN_WINDOW)) {
      vars.scanWindowList.addScanWindow(vars.scanWindow);

    } else if (closingTagName.equals(MzMLTags.TAG_SCAN_WINDOW_LIST)) {
      if (tracker.inside(MzMLTags.TAG_SPECTRUM)) {
        vars.scan.setScanWindowList(vars.scanWindowList);
      }

    } else if (closingTagName.equals(MzMLTags.TAG_SCAN)) {
      if (tracker.inside(MzMLTags.TAG_SPECTRUM)) {
        vars.spectrum.getScanList().addScan(vars.scan);
      }

    } else if (tracker.inside(MzMLTags.TAG_SPECTRUM_LIST)) {
      if (closingTagName.contentEquals(MzMLTags.TAG_SPECTRUM)) {
        filterProcessFinalizeScan();
      }
    }
    if (closingTagName.contentEquals(MzMLTags.TAG_SPECTRUM_LIST)) {
      // finished the last scan
      vars.memoryMapAndClearFrameMobilityScanData(storage);
    } else if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM_LIST)) {
      if (closingTagName.contentEquals(MzMLTags.TAG_CHROMATOGRAM)) {
        if (vars.chromatogram.getRtBinaryDataInfo() != null
            && vars.chromatogram.getIntensityBinaryDataInfo() != null && (newRawFile != null)) {
          vars.chromatogramsList.add(vars.chromatogram);
        }
      }
    }
  }

  /**
   * Called when spectrum end is read. Check if spectrum is filtered - skip this scan if not in
   * filter. Then process data points and memory map resulting data to disk to save RAM.
   */
  private void filterProcessFinalizeScan() {
    var spectrum = vars.spectrum;
//    logger.info(STR."Finalizing scan \{spectrum.getScanNumber()}");
    if (spectrum.isUVSpectrum()) {
      if (spectrum.loadProcessMemMapUvData(storage, scanProcessorConfig)) {
        vars.addSpectrumToList(storage, spectrum);
      }
      vars.spectrum = null;
      return;
    }

    if (scanProcessorConfig.scanFilter().matches(spectrum)) {
      if (spectrum.loadProcessMemMapMzData(storage, scanProcessorConfig)) {
        vars.addSpectrumToList(storage, spectrum);
      }
    }
    vars.spectrum = null;
  }

  /**
   * <p>
   * Call this method when the <code>xmlStreamReader</code> enters <code>&lt;cvParam&gt;</code> tag
   * </p>
   *
   * @param xmlStreamReader an instance of {@link XMLStreamReader XMLStreamReader
   * @return {@link MzMLCVParam MzMLCVParam} object notation of the <code>&lt;cvParam&gt;</code>
   * entered
   */
  private MzMLCVParam createMzMLCVParam(XMLStreamReader xmlStreamReader) {
    String accession = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_ACCESSION);
    String value = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_VALUE);
    String name = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_NAME);
    String unitAccession = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_UNIT_ACCESSION);

    // accession is a required attribute
    if (accession == null) {
      throw new IllegalStateException("Any cvParam must have an accession.");
    }

    // these attributes are optional
    return new MzMLCVParam(accession, value, name, unitAccession);
  }


  /**
   * <p>
   * getScanNumber.
   * </p>
   *
   * @param spectrumId a {@link String} object.
   * @return a {@link Integer} object.
   */
  public Optional<Integer> getScanNumber(String spectrumId) {
    final Matcher matcher = scanNumberPattern.matcher(spectrumId);
    boolean scanNumberFound = matcher.find();

    // Some vendors include scan=XX in the ID, some don't, such as
    // mzML converted from WIFF files. See the definition of nativeID in
    // http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo
    // So, get the value of the index tag if the scanNumber is not present in the ID
    if (scanNumberFound) {
      Integer scanNumber = Integer.parseInt(matcher.group(1));
      return Optional.of(scanNumber);
    }

    // agilent
    final Matcher agilentMatcher = agilentScanNumberPattern.matcher(spectrumId);
    boolean agilentScanNumberFound = agilentMatcher.find();
    if (agilentScanNumberFound) {
      Integer scanNumber = Integer.parseInt(agilentMatcher.group(1));
      return Optional.of(scanNumber);
    }

    return Optional.empty();
  }

  /**
   * <p>
   * Gets the required attribute from xmlStreamReader, throws an exception if the attribute is not
   * found
   * </p>
   *
   * @param xmlStreamReader XMLStreamReader instance used to parse
   * @param attr            Attribute's value to be found
   * @return a String containing the value of the attribute.
   */
  public String getRequiredAttribute(XMLStreamReader xmlStreamReader, String attr) {
    String attrValue = xmlStreamReader.getAttributeValue(null, attr);
    if (attrValue == null) {
      throw new IllegalStateException(
          "Tag " + xmlStreamReader.getLocalName() + " must provide an `" + attr
              + "`attribute (Line " + xmlStreamReader.getLocation().getLineNumber() + ")");
    }
    return attrValue;
  }

  /**
   * <p>
   * manageCompression.
   * </p>
   *
   * @param binaryInfo a {@link MzMLBinaryDataInfo} object.
   * @param accession  a {@link String} object.
   */
  public void manageCompression(MzMLBinaryDataInfo binaryInfo, String accession) {
    MzMLCompressionType newCompression = compressionTypeMap.get(accession);
    if (newCompression == null) {
      return;
    }
    if (binaryInfo.getCompressionType() == MzMLCompressionType.NO_COMPRESSION) {
      binaryInfo.setCompressionType(newCompression);
    } else {
      if (newCompression == MzMLCompressionType.ZLIB) {
        switch (binaryInfo.getCompressionType()) {
          case NUMPRESS_LINPRED ->
              binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_LINPRED_ZLIB);
          case NUMPRESS_POSINT ->
              binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_POSINT_ZLIB);
          case NUMPRESS_SHLOGF ->
              binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_SHLOGF_ZLIB);
        }
      } else {
        switch (newCompression) {
          case NUMPRESS_LINPRED ->
              binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_LINPRED_ZLIB);
          case NUMPRESS_POSINT ->
              binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_POSINT_ZLIB);
          case NUMPRESS_SHLOGF ->
              binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_SHLOGF_ZLIB);
        }
      }
    }
  }

  /**
   * <p>
   * getMzMLRawFile.
   * </p>
   *
   * @return a {@link MzMLRawDataFile MzMLRawDataFile} containing the parsed data
   */
  public MzMLRawDataFile getMzMLRawFile() {
    final List<BuildingMzMLMsScan> msSpectra = vars.spectrumList.stream()
        .filter(BuildingMzMLMsScan::isMassSpectrum).toList();
    newRawFile.setMsScans(msSpectra);
    newRawFile.setOtherScans(
        vars.spectrumList.stream().filter(scan -> !scan.isMassSpectrum()).toList());
    return newRawFile;
  }

  /**
   * Already memory mapped data of all scans
   *
   * @return
   */
  public List<BuildingMobilityScanStorage> getMobilityScanData() {
    return vars.mobilityScanData;
  }

  public int getTotalScans() {
    return totalScans;
  }

  public int getParsedScans() {
    return parsedScans;
  }

  public Float getFinishedPercentage() {
    if (totalScans == 0) {
      return 0.0f;
    }
    if (parsedScans > totalScans) {
      return 1.0f;
    }
    return ((float) parsedScans) / totalScans;
  }

  /**
   * Static class for holding temporary instances of variables initialized while parsing
   */
  private static class Vars {

    final List<BuildingMobilityScanStorage> mobilityScanData = new ArrayList<>();
    List<BuildingMzMLMsScan> spectrumList;
    int defaultArrayLength;
    boolean skipBinaryDataArray;
    BuildingMzMLMsScan spectrum;
    MzMLChromatogram chromatogram;
    MzMLBinaryDataInfo binaryDataInfo;
    MzMLReferenceableParamGroup referenceableParamGroup;
    MzMLPrecursorElement precursor;
    MzMLProduct product;
    MzMLIsolationWindow isolationWindow;
    MzMLPrecursorSelectedIonList selectedIonList;
    MzMLPrecursorSelectedIon selectedIon;
    MzMLPrecursorActivation activation;
    MzMLScan scan;
    MzMLScanWindowList scanWindowList;
    MzMLScanWindow scanWindow;
    ArrayList<MzMLReferenceableParamGroup> referenceableParamGroupList;
    List<BuildingMzMLMsScan> mobilityScans;
    List<Chromatogram> chromatogramsList;
    List<String> msFunctionsList;

    int nextFrameStartScanIndex = 0;

    Vars() {
      defaultArrayLength = 0;
      skipBinaryDataArray = false;
      spectrum = null;
      chromatogram = null;
      binaryDataInfo = null;
      referenceableParamGroup = null;
      precursor = null;
      product = null;
      isolationWindow = null;
      selectedIonList = null;
      selectedIon = null;
      activation = null;
      scan = null;
      scanWindowList = null;
      scanWindow = null;
      referenceableParamGroupList = new ArrayList<>();
      spectrumList = new ArrayList<>();
      mobilityScans = new ArrayList<>();
      chromatogramsList = new ArrayList<>();
      msFunctionsList = new ArrayList<>(); // TODO populate this list
    }

    public void addSpectrumToList(final MemoryMapStorage storage, BuildingMzMLMsScan scan) {
      MzMLMobility mobility = scan.getMobility();
      if (mobility == null) {
        // scan or frame spectrum
        spectrumList.add(scan);
        return;
      }

      if (mobilityScans.isEmpty()) {
        mobilityScans.add(scan);
        return;
      }

      BuildingMzMLMsScan last = mobilityScans.getLast();
      if (last != null && Double.compare(last.getRetentionTime(), scan.getRetentionTime()) != 0) {
        // changed retention time --> finish frame and memory map all mobility scans together as one
        memoryMapAndClearFrameMobilityScanData(storage);
      }
      mobilityScans.add(scan);
    }

    /**
     * Memory map all latest mobility scans into one data storage
     */
    public void memoryMapAndClearFrameMobilityScanData(final MemoryMapStorage storage) {
      if (mobilityScans.isEmpty()) {
        return;
      }

      // memory map data now to disk
      var memoryMapped = new BuildingMobilityScanStorage(storage, mobilityScans);
      mobilityScanData.add(memoryMapped);

      nextFrameStartScanIndex = mobilityScans.size();
      // all scans were already converted
      mobilityScans.clear();
    }
  }
}
