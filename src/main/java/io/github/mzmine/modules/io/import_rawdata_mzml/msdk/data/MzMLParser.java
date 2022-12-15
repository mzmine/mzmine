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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.MzMLFileImportMethod;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.TagTracker;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javolution.text.CharArray;
import javolution.xml.internal.stream.XMLStreamReaderImpl;
import javolution.xml.stream.XMLStreamReader;
import org.apache.commons.io.IOUtils;

/**
 * <p>
 * Used to parse mzML meta-data and initialize {@link MzMLBinaryDataInfo MzMLBinaryDataInfo}
 * </p>
 */
public class MzMLParser {

  private Vars vars;
  private TagTracker tracker;
  private final MzMLRawDataFile newRawFile;
  private final MzMLFileImportMethod importer;
  private int totalScans = 0, parsedScans = 0;
  private static final Logger logger = Logger.getLogger(MzMLParser.class.getName());

  /**
   * <p>
   * Constructor for {@link MzMLParser MzMLParser}
   * </p>
   *
   * @param importer an instance of an initialized {@link MzMLFileImportMethod
   *                 MzMLFileImportMethod}
   */
  public MzMLParser(MzMLFileImportMethod importer) {
    this.vars = new Vars();
    this.tracker = new TagTracker();
    this.importer = importer;
    this.newRawFile = new MzMLRawDataFile(importer.getMzMLFile(), vars.msFunctionsList,
        vars.spectrumList, vars.chromatogramsList);
  }

  /**
   * <p>
   * Carry out the required parsing of the mzML data when the {@link XMLStreamReaderImpl
   * XMLStreamReaderImpl} enters the given tag
   * </p>
   *
   * @param xmlStreamReader an instance of {@link XMLStreamReaderImpl XMLStreamReaderImpl
   * @param is              {@link InputStream InputStream} of the mzML data
   * @param openingTagName  The tag <code>xmlStreamReader</code> entered
   */
  public void processOpeningTag(XMLStreamReaderImpl xmlStreamReader, InputStream is,
      CharArray openingTagName) {
    tracker.enter(openingTagName);

    if (tracker.current().contentEquals((MzMLTags.TAG_RUN))) {
      final CharArray defaultInstrumentConfigurationRef = getRequiredAttribute(xmlStreamReader,
          MzMLTags.ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF);
      newRawFile.setDefaultInstrumentConfiguration(defaultInstrumentConfigurationRef.toString());

      // startTimeStamp may be optional, so it makes no sense to stop import of a RawDataFile
      // if this tag is skipped
      final CharArray startTimeStamp = xmlStreamReader.getAttributeValue(null,
          MzMLTags.ATTR_START_TIME_STAMP);
      if (startTimeStamp != null) {
        newRawFile.setStartTimeStamp(startTimeStamp.toString());
        logger.info("startTimeStamp value is: " + startTimeStamp);
      } else {
        logger.info("startTimeStamp wasn't set");
      }
    }

    if (tracker.current().contentEquals((MzMLTags.TAG_SPECTRUM_LIST))) {
      final CharArray defaultDataProcessingRefScan = getRequiredAttribute(xmlStreamReader,
          MzMLTags.ATTR_DEFAULT_DATA_PROCESSING_REF);
      newRawFile.setDefaultDataProcessingScan(defaultDataProcessingRefScan.toString());
    }

    if (tracker.current().contentEquals((MzMLTags.TAG_CHROMATOGRAM_LIST))) {
      final CharArray defaultDataProcessingRefChromatogram = getRequiredAttribute(xmlStreamReader,
          MzMLTags.ATTR_DEFAULT_DATA_PROCESSING_REF);
      newRawFile.setDefaultDataProcessingScan(defaultDataProcessingRefChromatogram.toString());
    }

    if (openingTagName.contentEquals((MzMLTags.TAG_SPECTRUM_LIST))) {
      final CharArray count = getRequiredAttribute(xmlStreamReader, "count");
      this.totalScans = count.toInt();
    }

    if (tracker.inside(MzMLTags.TAG_REF_PARAM_GROUP_LIST)) {

      if (openingTagName.contentEquals(MzMLTags.TAG_REF_PARAM_GROUP)) {
        final CharArray id = getRequiredAttribute(xmlStreamReader, "id");
        vars.referenceableParamGroup = new MzMLReferenceableParamGroup(id.toString());

      } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
        MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
        vars.referenceableParamGroup.addCVParam(cvParam);

      }
    }

    if (tracker.inside(MzMLTags.TAG_SPECTRUM_LIST)) {
      if (openingTagName.contentEquals(MzMLTags.TAG_SPECTRUM)) {
        String id = getRequiredAttribute(xmlStreamReader, "id").toString();
        Integer index = getRequiredAttribute(xmlStreamReader, "index").toInt();
        vars.defaultArrayLength = getRequiredAttribute(xmlStreamReader,
            "defaultArrayLength").toInt();
        Integer scanNumber = getScanNumber(id).orElse(index + 1);
        vars.spectrum = new MzMLMsScan(newRawFile, is, id, scanNumber, vars.defaultArrayLength);


      } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY_DATA_ARRAY)) {
        vars.skipBinaryDataArray = false;
        int encodedLength = getRequiredAttribute(xmlStreamReader, "encodedLength").toInt();
        final CharArray arrayLength = xmlStreamReader.getAttributeValue(null, "arrayLength");
        if (arrayLength != null) {
          vars.binaryDataInfo = new MzMLBinaryDataInfo(encodedLength, arrayLength.toInt());
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
          String accession = getRequiredAttribute(xmlStreamReader, "accession").toString();
          if (vars.binaryDataInfo.isBitLengthAccession(accession)) {
            vars.binaryDataInfo.setBitLength(accession);
          } else if (vars.binaryDataInfo.isCompressionTypeAccession(accession)) {
            manageCompression(vars.binaryDataInfo, accession);
          } else if (vars.binaryDataInfo.isArrayTypeAccession(accession)) {
            vars.binaryDataInfo.setArrayType(accession);
          } else {
            vars.skipBinaryDataArray = true;
          }

        }


      } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY)) {
        if (vars.spectrum != null && !vars.skipBinaryDataArray) {
          int bomOffset = xmlStreamReader.getLocation().getBomLength();
          vars.binaryDataInfo.setPosition(
              xmlStreamReader.getLocation().getTotalCharsRead() + bomOffset);
        }
        if (!vars.skipBinaryDataArray) {
          if (MzMLCV.cvMzArray.equals(vars.binaryDataInfo.getArrayType().getAccession())) {
            vars.spectrum.setMzBinaryDataInfo(vars.binaryDataInfo);
          }
          if (MzMLCV.cvIntensityArray.equals(vars.binaryDataInfo.getArrayType().getAccession())) {
            vars.spectrum.setIntensityBinaryDataInfo(vars.binaryDataInfo);
          }
        }


      } else if (openingTagName.contentEquals(MzMLTags.TAG_REF_PARAM_GROUP_REF)) {
        String refValue = getRequiredAttribute(xmlStreamReader, "ref").toString();
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
          final CharArray spectrumRef = xmlStreamReader.getAttributeValue(null,
              MzMLTags.ATTR_SPECTRUM_REF);
          String spectrumRefString = spectrumRef == null ? null : spectrumRef.toString();
          vars.precursor = new MzMLPrecursorElement(spectrumRefString);

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
      if (openingTagName.contentEquals(MzMLTags.TAG_CHROMATOGRAM)) {
        String chromatogramId = getRequiredAttribute(xmlStreamReader, "id").toString();
        Integer chromatogramNumber = getRequiredAttribute(xmlStreamReader, "index").toInt() + 1;
        vars.defaultArrayLength = getRequiredAttribute(xmlStreamReader,
            "defaultArrayLength").toInt();
        vars.chromatogram = new MzMLChromatogram(newRawFile, is, chromatogramId, chromatogramNumber,
            vars.defaultArrayLength);

      } else if (openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)) {
        if (!tracker.inside(MzMLTags.TAG_BINARY_DATA_ARRAY) && !tracker.inside(
            MzMLTags.TAG_PRECURSOR) && !tracker.inside(MzMLTags.TAG_PRODUCT)
            && vars.chromatogram != null) {
          MzMLCVParam cvParam = createMzMLCVParam(xmlStreamReader);
          vars.chromatogram.getCVParams().addCVParam(cvParam);
          ;
        }

      } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY_DATA_ARRAY)) {
        vars.skipBinaryDataArray = false;
        int encodedLength = getRequiredAttribute(xmlStreamReader, "encodedLength").toInt();
        final CharArray arrayLength = xmlStreamReader.getAttributeValue(null, "arrayLength");
        if (arrayLength != null) {
          vars.binaryDataInfo = new MzMLBinaryDataInfo(encodedLength, arrayLength.toInt());
        } else {
          vars.binaryDataInfo = new MzMLBinaryDataInfo(encodedLength, vars.defaultArrayLength);
        }

      } else if (openingTagName.contentEquals(MzMLTags.TAG_BINARY)) {
        if (vars.chromatogram != null && !vars.skipBinaryDataArray) {
          int bomOffset = xmlStreamReader.getLocation().getBomLength();
          vars.binaryDataInfo.setPosition(
              xmlStreamReader.getLocation().getTotalCharsRead() + bomOffset);
        }
        if (!vars.skipBinaryDataArray) {
          if (MzMLCV.cvRetentionTimeArray.equals(
              vars.binaryDataInfo.getArrayType().getAccession())) {
            vars.chromatogram.setRtBinaryDataInfo(vars.binaryDataInfo);
            ;
          }
          if (MzMLCV.cvIntensityArray.equals(vars.binaryDataInfo.getArrayType().getAccession())) {
            vars.chromatogram.setIntensityBinaryDataInfo(vars.binaryDataInfo);
          }
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

      if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM) && tracker.inside(
          MzMLTags.TAG_BINARY_DATA_ARRAY) && openingTagName.contentEquals(MzMLTags.TAG_CV_PARAM)
          && vars.binaryDataInfo != null && !vars.skipBinaryDataArray) {
        String accession = getRequiredAttribute(xmlStreamReader, "accession").toString();
        if (vars.binaryDataInfo.isBitLengthAccession(accession)) {
          vars.binaryDataInfo.setBitLength(accession);
        } else if (vars.binaryDataInfo.isCompressionTypeAccession(accession)) {
          manageCompression(vars.binaryDataInfo, accession);
        } else if (vars.binaryDataInfo.isArrayTypeAccession(accession)) {
          vars.binaryDataInfo.setArrayType(accession);
        } else {
          vars.skipBinaryDataArray = true;
        }

      }

      if (openingTagName.contentEquals(MzMLTags.TAG_PRECURSOR)) {
        final CharArray spectrumRef = xmlStreamReader.getAttributeValue(null, "spectrumRef");
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
  }

  private MzMLUserParam createMzMLUserParam(XMLStreamReaderImpl xmlStreamReader) {
    CharArray name = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_NAME);
    CharArray value = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_VALUE);
    if (name != null && value != null) {
      return new MzMLUserParam(name.toString(), value.toString());
    }
    return null;
  }

  /**
   * <p>
   * Carry out the required parsing of the mzML data when the {@link XMLStreamReaderImpl
   * XMLStreamReaderImpl} exits the given tag
   * </p>
   *
   * @param xmlStreamReader an instance of {@link XMLStreamReaderImpl XMLStreamReaderImpl
   * @param closingTagName  a {@link CharArray} object.
   */
  public void processClosingTag(XMLStreamReaderImpl xmlStreamReader, CharArray closingTagName) {
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
        if (vars.spectrum.getMzBinaryDataInfo() != null
            && vars.spectrum.getIntensityBinaryDataInfo() != null && (importer.getMzMLFile() != null
            || importer.getMsScanPredicate().test(vars.spectrum))) {
          vars.spectrumList.add(vars.spectrum);
        }
      }

    } else if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM_LIST)) {
      if (closingTagName.contentEquals(MzMLTags.TAG_CHROMATOGRAM)) {
        if (vars.chromatogram.getRtBinaryDataInfo() != null
            && vars.chromatogram.getIntensityBinaryDataInfo() != null && (
            importer.getMzMLFile() != null || importer.getChromatogramPredicate()
                .test(vars.chromatogram))) {
          vars.chromatogramsList.add(vars.chromatogram);
        }
      }

    }

  }

  /**
   * <p>
   * Carry out the required parsing of the mzML data when the {@link XMLStreamReaderImpl
   * XMLStreamReaderImpl} when {@link javolution.xml.stream.XMLStreamConstants#CHARACTERS
   * CHARACTERS} are found
   * </p>
   *
   * @param xmlStreamReader an instance of {@link XMLStreamReaderImpl XMLStreamReaderImpl
   */
  public void processCharacters(XMLStreamReaderImpl xmlStreamReader) {
    if (!newRawFile.getOriginalFile().isPresent() && tracker.current()
        .contentEquals(MzMLTags.TAG_BINARY) && !vars.skipBinaryDataArray) {
      if (tracker.inside(MzMLTags.TAG_SPECTRUM_LIST) && importer.getMsScanPredicate()
          .test(vars.spectrum)) {
        vars.spectrum.setInputStream(IOUtils.toInputStream(xmlStreamReader.getText()));
        switch (vars.binaryDataInfo.getArrayType().getAccession()) {
          case MzMLCV.cvMzArray:
            vars.spectrum.getMzValues();
            break;

          case MzMLCV.cvIntensityArray:
            vars.spectrum.getIntensityValues();
            break;
        }
      } else if (tracker.inside(MzMLTags.TAG_CHROMATOGRAM_LIST)
          && importer.getChromatogramPredicate().test(vars.chromatogram)) {
        vars.chromatogram.setInputStream(IOUtils.toInputStream(xmlStreamReader.getText()));
        switch (vars.binaryDataInfo.getArrayType().getAccession()) {
          case MzMLCV.cvRetentionTimeArray:
            vars.chromatogram.getRetentionTimes();
            break;

          case MzMLCV.cvIntensityArray:
            vars.chromatogram.getIntensityBinaryDataInfo();
            break;
        }
      }
    }
  }

  /**
   * <p>
   * Call this method when the <code>xmlStreamReader</code> enters <code>&lt;cvParam&gt;</code> tag
   * </p>
   *
   * @param xmlStreamReader an instance of {@link XMLStreamReaderImpl XMLStreamReaderImpl
   * @return {@link MzMLCVParam MzMLCVParam} object notation of the <code>&lt;cvParam&gt;</code>
   * entered
   */
  private MzMLCVParam createMzMLCVParam(XMLStreamReader xmlStreamReader) {
    CharArray accession = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_ACCESSION);
    CharArray value = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_VALUE);
    CharArray name = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_NAME);
    CharArray unitAccession = xmlStreamReader.getAttributeValue(null, MzMLTags.ATTR_UNIT_ACCESSION);

    // accession is a required attribute
    if (accession == null) {
      throw new IllegalStateException("Any cvParam must have an accession.");
    }

    // these attributes are optional
    String valueStr = value == null ? null : value.toString();
    String nameStr = name == null ? null : name.toString();
    String unitAccessionStr = unitAccession == null ? null : unitAccession.toString();

    return new MzMLCVParam(accession.toString(), valueStr, nameStr, unitAccessionStr);
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

    // agilent
    final Pattern agilentPattern = Pattern.compile("scan[iI]d=([0-9]+)");
    final Matcher agilentMatcher = agilentPattern.matcher(spectrumId);
    boolean agilentScanNumberFound = agilentMatcher.find();
    if (agilentScanNumberFound) {
      Integer scanNumber = Integer.parseInt(agilentMatcher.group(1));
      return Optional.ofNullable(scanNumber);
    }

    return Optional.ofNullable(null);
  }

  /**
   * <p>
   * Gets the required attribute from xmlStreamReader, throws an exception if the attribute is not
   * found
   * </p>
   *
   * @param xmlStreamReader XMLStreamReader instance used to parse
   * @param attr            Attribute's value to be found
   * @return a CharArray containing the value of the attribute.
   */
  public CharArray getRequiredAttribute(XMLStreamReader xmlStreamReader, String attr) {
    CharArray attrValue = xmlStreamReader.getAttributeValue(null, attr);
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
    if (binaryInfo.getCompressionType() == MzMLCompressionType.NO_COMPRESSION) {
      binaryInfo.setCompressionType(accession);
    } else {
      if (binaryInfo.getCompressionType(accession) == MzMLCompressionType.ZLIB) {
        switch (binaryInfo.getCompressionType()) {
          case NUMPRESS_LINPRED:
            binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_LINPRED_ZLIB);
            break;
          case NUMPRESS_POSINT:
            binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_POSINT_ZLIB);
            break;
          case NUMPRESS_SHLOGF:
            binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_SHLOGF_ZLIB);
            break;
          default:
            break;
        }
      } else {
        switch (binaryInfo.getCompressionType(accession)) {
          case NUMPRESS_LINPRED:
            binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_LINPRED_ZLIB);
            break;
          case NUMPRESS_POSINT:
            binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_POSINT_ZLIB);
            break;
          case NUMPRESS_SHLOGF:
            binaryInfo.setCompressionType(MzMLCompressionType.NUMPRESS_SHLOGF_ZLIB);
            break;
          default:
            break;
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
    return newRawFile;
  }

  /**
   * Static class for holding temporary instances of variables initialized while parsing
   */
  private static class Vars {

    int defaultArrayLength;
    boolean skipBinaryDataArray;
    MzMLMsScan spectrum;
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
    List<MsScan> spectrumList;
    List<Chromatogram> chromatogramsList;
    List<String> msFunctionsList;

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
      chromatogramsList = new ArrayList<>();
      msFunctionsList = new ArrayList<>(); // TODO populate this list
    }
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

}
