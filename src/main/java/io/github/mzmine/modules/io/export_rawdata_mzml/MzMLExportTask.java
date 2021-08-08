/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.export_rawdata_mzml;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.ActivationInfo;
import io.github.mzmine.datamodel.Chromatogram;
import io.github.mzmine.datamodel.IsolationInfo;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLArrayType;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLBitLength;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLCVGroup;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLCompressionType;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLMsScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLPeaksEncoder;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLPrecursorSelectedIon;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLProduct;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLRawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLTags;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.FileOutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javolution.xml.internal.stream.XMLStreamWriterImpl;
import javolution.xml.stream.XMLStreamException;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

public class MzMLExportTask extends AbstractTask {

  private static final String dataProcessingId = "MSDK_mzml_export";
  private static final String softwareId = "MSDK";
  private static final String XML_ENCODING = "UTF-8";
  private static final String XML_VERSION = "1.0";
  private static final String MZML_NAMESPACE = "http://psi.hupo.org/ms/mzml";
  private static final String XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String XML_SCHEMA_LOCATION =
      "http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0.xsd";
  private static final String DEFAULT_VERSION = "1.1.0";
  private static final String CV_REF_MS = "MS";
  private static final String PREFIX_XSI = "xsi";

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private final RawDataFile dataFile;

  private final @NotNull MzMLCompressionType doubleArrayCompression = MzMLCompressionType.ZLIB;
  private final @NotNull MzMLCompressionType floatArrayCompression = MzMLCompressionType.ZLIB;

  // User parameters
  private File outFilename;

  private long totalScans = 0, totalChromatograms = 0, parsedScans, parsedChromatograms,
      indexListOffset;


  /**
   * @param dataFile
   */
  public MzMLExportTask(RawDataFile dataFile, File outFilename) {
    super(null); // no new data stored -> null
    this.dataFile = dataFile;
    this.outFilename = outFilename;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Exporting file " + dataFile + " to " + outFilename;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    return (totalScans + totalChromatograms) == 0 ? 0.0
        : (double) (parsedScans + parsedChromatograms) / (totalScans + totalChromatograms);
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    try {

      setStatus(TaskStatus.PROCESSING);

      logger.info("Started export of file " + dataFile + " to " + outFilename);

      List<Scan> scans = dataFile.getScans();
      List<Chromatogram> chromatograms = dataFile.getChromatograms();
      totalScans = scans.size();
      totalChromatograms = chromatograms.size();

      // index offsets
      List<Long> spectrumIndices = new ArrayList<>();
      List<Long> chromatogramIndices = new ArrayList<>();

      FileOutputStream fos = new FileOutputStream(outFilename);
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      DigestOutputStream dos = new DigestOutputStream(fos, sha1);
      dos.on(true);
      XMLStreamWriterImpl xmlStreamWriter = new XMLStreamWriterImpl();
      xmlStreamWriter.setOutput(dos);

      // Setting namespace and prefixes
      xmlStreamWriter.setDefaultNamespace(MZML_NAMESPACE);
      xmlStreamWriter.setPrefix(PREFIX_XSI, XML_SCHEMA_INSTANCE);

      // <?xml>
      xmlStreamWriter.writeStartDocument(XML_ENCODING, XML_VERSION);

      // <indexedmzML>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_INDEXED_MZML);
      xmlStreamWriter.writeDefaultNamespace(MZML_NAMESPACE);
      xmlStreamWriter.writeNamespace(PREFIX_XSI, XML_SCHEMA_INSTANCE);
      xmlStreamWriter.writeAttribute(XML_SCHEMA_INSTANCE, MzMLTags.ATTR_SCHEME_LOCATION,
          XML_SCHEMA_LOCATION);

      // <mzML>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_MZML);
      xmlStreamWriter.writeDefaultNamespace(MZML_NAMESPACE);
      xmlStreamWriter.writeNamespace(PREFIX_XSI, XML_SCHEMA_INSTANCE);
      xmlStreamWriter.writeAttribute(XML_SCHEMA_INSTANCE, MzMLTags.ATTR_SCHEME_LOCATION,
          XML_SCHEMA_LOCATION);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ID, dataFile.getName());
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_VERSION, DEFAULT_VERSION);

      // <cvList>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_CV_LIST);
      // TODO: Hold cvList in the existing RawDataFile model
      xmlStreamWriter.writeEndElement();

      // <dataProcessingList>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_DATA_PROCESSING_LIST);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, "1");

      // <dataProcessing>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_DATA_PROCESSING);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ID, dataProcessingId);

      // <processingMethod>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_PROCESSING_METHOD);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_SOFTWARE_REF, softwareId);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ORDER, "0");

      // Closing tags
      xmlStreamWriter.writeEndElement(); // </processingMethod>
      xmlStreamWriter.writeEndElement(); // </dataProcessing>
      xmlStreamWriter.writeEndElement(); // </dataProcessingList>

      // <run>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_RUN);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ID, dataFile.getName());
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF,
          dataFile instanceof MzMLRawDataFile
              ? ((MzMLRawDataFile) dataFile).getDefaultInstrumentConfiguration()
              : "unknown");

      // <spectrumList>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_SPECTRUM_LIST);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, String.valueOf(scans.size()));
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_DEFAULT_DATA_PROCESSING_REF,
          dataFile instanceof MzMLRawDataFile
              ? ((MzMLRawDataFile) dataFile).getDefaultDataProcessingScan()
              : "unknown");

      byte[] mzBuffer = null;
      byte[] intensityBuffer = null;

      for (Scan scan : scans) {

        if (isCanceled()) {
          dos.close();
          fos.close();
          xmlStreamWriter.close();
          outFilename.delete();
          return;
        }

        // <spectrum>
        spectrumIndices.add(xmlStreamWriter.getLocation().getCharacterOffsetInLong());
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_SPECTRUM);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_INDEX, String.valueOf(parsedScans));
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ID, "scan=" + scan.getScanNumber());
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_DEFAULT_ARRAY_LENGTH,
            String.valueOf(scan.getNumberOfDataPoints()));

        MzMLMsScan spectrum = null;
        if (scan instanceof MzMLMsScan) {
          spectrum = (MzMLMsScan) scan;
        }

        // spectrum type CV param
        if (!(dataFile instanceof MzMLRawDataFile) || (dataFile instanceof MzMLRawDataFile
            && !spectrum.getCVValue(MzMLCV.cvCentroidSpectrum).isPresent()
            && !spectrum.getCVValue(MzMLCV.cvProfileSpectrum).isPresent())) {
          if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
            writeCVParam(xmlStreamWriter, MzMLCV.centroidCvParam);
          } else {
            writeCVParam(xmlStreamWriter, MzMLCV.profileCvParam);
          }
        }

        // ms level CV param
        if (!(dataFile instanceof MzMLRawDataFile) || (dataFile instanceof MzMLRawDataFile
            && !spectrum.getCVValue(MzMLCV.cvMSLevel).isPresent())) {
          Integer msLevel = scan.getMSLevel();
          writeCVParam(xmlStreamWriter,
              new MzMLCVParam(MzMLCV.cvMSLevel, String.valueOf(msLevel), "ms level", null));
        }

        // total ion current CV param
        if (!(dataFile instanceof MzMLRawDataFile) || (dataFile instanceof MzMLRawDataFile
            && !spectrum.getCVValue(MzMLCV.cvTIC).isPresent())) {
          if (scan.getTIC() != null) {
            Double tic = scan.getTIC();
            writeCVParam(xmlStreamWriter,
                new MzMLCVParam(MzMLCV.cvTIC, String.valueOf(tic), "total ion current", null));
          }
        }

        // m/z range CV param
        if (!(dataFile instanceof MzMLRawDataFile) || (dataFile instanceof MzMLRawDataFile
            && !spectrum.getCVValue(MzMLCV.cvLowestMz).isPresent()
            || !spectrum.getCVValue(MzMLCV.cvHighestMz).isPresent())) {
          if (scan.getDataPointMZRange() != null) {
            Double lowestMz = scan.getDataPointMZRange().lowerEndpoint();
            Double highestMz = scan.getDataPointMZRange().upperEndpoint();
            writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvLowestMz,
                String.valueOf(lowestMz), "lowest observed m/z", MzMLCV.cvMz));
            writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvHighestMz,
                String.valueOf(highestMz), "highest observed m/z", MzMLCV.cvMz));
          }
        }

        // Write the missing CV params parsed
        if (dataFile instanceof MzMLRawDataFile) {
          writeCVGroup(xmlStreamWriter, spectrum.getCVParams());
        }

        // <scanList>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_SCAN_LIST);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, "1");

        // <scan>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_SCAN);

        // scan definition CV param
        if (scan.getScanDefinition() != null) {
          String scanDefinition = scan.getScanDefinition();
          writeCVParam(xmlStreamWriter,
              new MzMLCVParam(MzMLCV.cvScanFilterString, scanDefinition, "filter string", null));
        }

        // retention time CV param
        Float rt = scan.getRetentionTime();
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.MS_RT_SCAN_START, String.valueOf(rt),
            "scan time", MzMLCV.cvUnitsSec));

        // scan polarity CV param
        if (scan.getPolarity() == PolarityType.POSITIVE) {
          writeCVParam(xmlStreamWriter, MzMLCV.polarityPositiveCvParam);
        } else if (scan.getPolarity() == PolarityType.NEGATIVE) {
          writeCVParam(xmlStreamWriter, MzMLCV.polarityNegativeCvParam);
        }

        // <scanWindowList>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_SCAN_WINDOW_LIST);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, "1");

        // <scanWindow>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_SCAN_WINDOW);

        // scan window range CV param
        if (scan.getScanningMZRange() != null) {
          Double lowerLimit = scan.getScanningMZRange().lowerEndpoint();
          Double upperLimit = scan.getScanningMZRange().upperEndpoint();
          writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvScanWindowLowerLimit,
              String.valueOf(lowerLimit), "scan window lower limit", MzMLCV.cvMz));
          writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvScanWindowUpperLimit,
              String.valueOf(upperLimit), "scan window upper limit", MzMLCV.cvMz));
        }

        // Closing tags
        xmlStreamWriter.writeEndElement(); // </scanWindow>
        xmlStreamWriter.writeEndElement(); // </scanWindowList>
        xmlStreamWriter.writeEndElement(); // </scan>
        xmlStreamWriter.writeEndElement(); // </scanList>

        if (dataFile instanceof MzMLRawDataFile
            && spectrum.getPrecursorList().getPrecursorElements().size() > 0) {

          // <precursorList>
          xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRECURSOR_LIST);
          xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT,
              String.valueOf(spectrum.getPrecursorList().getPrecursorElements().size()));

          for (MzMLPrecursorElement precursor : spectrum.getPrecursorList()
              .getPrecursorElements()) {

            // <precursor>
            xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRECURSOR);
            xmlStreamWriter.writeAttribute(MzMLTags.ATTR_SPECTRUM_REF,
                precursor.getSpectrumRef().orElse(""));

            if (precursor.getSelectedIonList().isPresent()) {

              // <slectedIonList>
              xmlStreamWriter.writeStartElement(MzMLTags.TAG_SELECTED_ION_LIST);
              xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT,
                  String.valueOf(precursor.getSelectedIonList().get().getSelectedIonList().size()));

              for (MzMLPrecursorSelectedIon selectedIon : precursor.getSelectedIonList().get()
                  .getSelectedIonList()) {

                // <selectedIon>
                xmlStreamWriter.writeStartElement(MzMLTags.TAG_SELECTED_ION);
                writeCVGroup(xmlStreamWriter, selectedIon);
                xmlStreamWriter.writeEndElement(); // </selectedIon>
              }
              xmlStreamWriter.writeEndElement(); // </selectedIonList>

            }

            if (precursor.getIsolationWindow().isPresent()) {

              // <isolationWindow>
              xmlStreamWriter.writeStartElement(MzMLTags.TAG_ISOLATION_WINDOW);
              writeCVGroup(xmlStreamWriter, precursor.getIsolationWindow().get());
              xmlStreamWriter.writeEndElement(); // </isolationWindow>
            }

            // <activation>
            xmlStreamWriter.writeStartElement(MzMLTags.TAG_ACTIVATION);
            writeCVGroup(xmlStreamWriter, precursor.getActivation());
            xmlStreamWriter.writeEndElement(); // </activation>

            xmlStreamWriter.writeEndElement(); // </precursor>

          }

          xmlStreamWriter.writeEndElement(); // </precursorList>

          if (!spectrum.getProductList().getProducts().isEmpty()) {

            // <productList>
            xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRODUCT_LIST);
            xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT,
                String.valueOf(spectrum.getProductList().getProducts().size()));

            for (MzMLProduct product : spectrum.getProductList().getProducts()) {
              // <product>
              xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRODUCT);

              // <isolationWindow>
              xmlStreamWriter.writeStartElement(MzMLTags.TAG_ISOLATION_WINDOW);
              if (product.getIsolationWindow().isPresent()) {
                writeCVGroup(xmlStreamWriter, product.getIsolationWindow().get());
              }
              xmlStreamWriter.writeEndElement(); // </isolationWindow>

              xmlStreamWriter.writeEndElement(); // </product>
            }

            xmlStreamWriter.writeEndElement(); // </productList>
          }
        }

        // TODO Once changes to IsolationWindow have been merged, we can start working on exporting
        // the precursor data for non-mzML data since we have all the required data

        // else if (scan.getIsolations().size() > 0) {
        // // <precursorList>
        // xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRECURSOR_LIST);
        // xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, "1");
        //
        // // <precursor>
        // xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRECURSOR);
        // xmlStreamWriter.writeAttribute(MzMLTags.ATTR_SPECTRUM_REF, "scan=" + scan.get);
        //
        // // <isolationWindow>
        // xmlStreamWriter.writeStartElement(MzMLTags.TAG_ISOLATION_WINDOW);
        //
        // }

        // <binaryDataArrayList>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY_DATA_ARRAY_LIST);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, "2");

        // <binaryDataArray> (m/z)
        mzBuffer = MzMLPeaksEncoder.encodeDouble(scan.getMzValues(null), doubleArrayCompression);
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY_DATA_ARRAY);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ENCODED_LENGTH,
            String.valueOf(mzBuffer.length));

        // data array precision CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLBitLength.SIXTY_FOUR_BIT_FLOAT.getValue(),
            "", "64-bit float", null));

        // data array compression CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(doubleArrayCompression.getAccession(), "",
            doubleArrayCompression.getName(), null));

        // data array type CV param
        writeCVParam(xmlStreamWriter,
            new MzMLCVParam(MzMLArrayType.MZ.getAccession(), "", "m/z array", MzMLCV.cvMz));

        // <binary>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY);
        xmlStreamWriter.writeCharacters(new String(mzBuffer));

        // Closing tags
        xmlStreamWriter.writeEndElement(); // </binary>
        xmlStreamWriter.writeEndElement(); // </binaryDataArray>

        // <binaryDataArray> (intensity)
        intensityBuffer =
            MzMLPeaksEncoder.encodeDouble(scan.getIntensityValues(null), doubleArrayCompression);
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY_DATA_ARRAY);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ENCODED_LENGTH,
            String.valueOf(intensityBuffer.length));

        // data array precision CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLBitLength.THIRTY_TWO_BIT_FLOAT.getValue(),
            "", "32-bit float", null));

        // data array compression CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(floatArrayCompression.getAccession(), "",
            floatArrayCompression.getName(), null));

        // data array type CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLArrayType.INTENSITY.getAccession(), "",
            "intensity array", MzMLCV.cvUnitsIntensity1));

        // <binary>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY);
        xmlStreamWriter.writeCharacters(new String(intensityBuffer));

        // Closing tags
        xmlStreamWriter.writeEndElement(); // </binary>
        xmlStreamWriter.writeEndElement(); // </binaryDataArray>
        xmlStreamWriter.writeEndElement(); // </binaryDataArrayList
        xmlStreamWriter.writeEndElement(); // </spectrum>

        parsedScans++;

      }

      xmlStreamWriter.writeEndElement(); // </spectrumList>

      // <chromatogramList>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_CHROMATOGRAM_LIST);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, String.valueOf(chromatograms.size()));
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_DEFAULT_DATA_PROCESSING_REF,
          dataFile instanceof MzMLRawDataFile
              ? ((MzMLRawDataFile) dataFile).getDefaultDataProcessingChromatogram()
              : "unknown");

      byte[] rtBuffer = null;
      byte[] intensityBuffer2 = null;

      for (Chromatogram chromatogram : chromatograms) {
        if (isCanceled()) {
          dos.close();
          fos.close();
          xmlStreamWriter.close();
          outFilename.delete();
          return;
        }

        // <chromatogram>
        chromatogramIndices.add(xmlStreamWriter.getLocation().getCharacterOffsetInLong());
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_CHROMATOGRAM);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_INDEX, String.valueOf(parsedChromatograms));
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ID, chromatogram.getChromatogramType().name());
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_DEFAULT_ARRAY_LENGTH,
            String.valueOf(chromatogram.getNumberOfDataPoints()));

        // chromatogram type CV param
        switch (chromatogram.getChromatogramType()) {
          case BPC:
            writeCVParam(xmlStreamWriter,
                new MzMLCVParam(MzMLCV.cvChromatogramBPC, "", "basepeak chromatogram", null));
            break;
          case MRM_SRM:
            writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvChromatogramMRM_SRM, "",
                "selected reaction monitoring chromatogram", null));
            break;
          case SIC:
            writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvChromatogramSIC, "",
                "selected ion current chromatogram", null));
            break;
          case TIC:
            writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvChromatogramTIC, "",
                "total ion current chromatogram", null));
            break;
          default:
            break;
        }

        // Isolation info
        if (!chromatogram.getIsolations().isEmpty()) {
          IsolationInfo isolationInfo = chromatogram.getIsolations().get(0);

          // <precursor>
          xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRECURSOR);

          if (isolationInfo.getPrecursorMz() != null) {
            // <isolationWindow>
            xmlStreamWriter.writeStartElement(MzMLTags.TAG_ISOLATION_WINDOW);

            Double mz = isolationInfo.getPrecursorMz();
            writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvIsolationWindowTarget,
                String.valueOf(mz), "isolation window outFilename m/z", MzMLCV.cvMz));

            xmlStreamWriter.writeEndElement(); // </isolationWindow>
          }

          if (isolationInfo.getActivationInfo() != null) {
            // <activation>
            xmlStreamWriter.writeStartElement(MzMLTags.TAG_ACTIVATION);

            ActivationInfo activationInfo = isolationInfo.getActivationInfo();

            switch (activationInfo.getActivationType()) {
              case CID:
                writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvActivationCID, "",
                    "collision-induced dissociation", null));
                break;
              default:
                break;
            }

            if (activationInfo.getActivationEnergy() != null) {
              writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvActivationEnergy,
                  String.valueOf(activationInfo.getActivationEnergy()), "collision energy", null));
            }

            xmlStreamWriter.writeEndElement(); // </activation>
          }

          xmlStreamWriter.writeEndElement(); // </precursor>
        }

        // product m/z value CV param
        if (chromatogram.getMz() != null) {
          // <product>
          xmlStreamWriter.writeStartElement(MzMLTags.TAG_PRODUCT);

          // <isolationWindow>
          xmlStreamWriter.writeStartElement(MzMLTags.TAG_ISOLATION_WINDOW);

          Double mz = chromatogram.getMz();
          writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLCV.cvIsolationWindowTarget,
              String.valueOf(mz), "isolation window outFilename m/z", MzMLCV.cvMz));

          // Closing tags
          xmlStreamWriter.writeEndElement(); // </isolationWindow>
          xmlStreamWriter.writeEndElement(); // </product>
        }

        // <binaryDataArrayList>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY_DATA_ARRAY_LIST);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, "2");

        // <binaryDataArray> (time)
        rtBuffer = MzMLPeaksEncoder.encodeFloat(chromatogram.getRetentionTimes(null),
            floatArrayCompression);
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY_DATA_ARRAY);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ENCODED_LENGTH,
            String.valueOf(rtBuffer.length));

        // data array precision CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLBitLength.THIRTY_TWO_BIT_FLOAT.getValue(),
            "", "32-bit float", null));

        // data array compression CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(floatArrayCompression.getAccession(), "",
            floatArrayCompression.getName(), null));

        // data array type CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLArrayType.TIME.getAccession(), "",
            "time array", MzMLCV.cvUnitsMin2));

        // <binary>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY);
        xmlStreamWriter.writeCharacters(new String(rtBuffer));

        // Closing tags
        xmlStreamWriter.writeEndElement(); // </binary>
        xmlStreamWriter.writeEndElement(); // </binaryDataArray>

        // <binaryDataArray> (intensity)
        intensityBuffer2 =
            MzMLPeaksEncoder.encodeFloat(chromatogram.getIntensityValues(), floatArrayCompression);
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY_DATA_ARRAY);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ENCODED_LENGTH,
            String.valueOf(intensityBuffer2.length));

        // data array precision CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLBitLength.THIRTY_TWO_BIT_FLOAT.getValue(),
            "", "32-bit float", null));

        // data array compression CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(floatArrayCompression.getAccession(), "",
            floatArrayCompression.getName(), null));

        // data array type CV param
        writeCVParam(xmlStreamWriter, new MzMLCVParam(MzMLArrayType.INTENSITY.getAccession(), "",
            "intensity array", MzMLCV.cvUnitsIntensity1));

        // <binary>
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_BINARY);
        xmlStreamWriter.writeCharacters(new String(intensityBuffer2));

        // Closing tags
        xmlStreamWriter.writeEndElement(); // </binary>
        xmlStreamWriter.writeEndElement(); // </binaryDataArray>
        xmlStreamWriter.writeEndElement(); // </binaryDataArrayList
        xmlStreamWriter.writeEndElement(); // </chromatogram>

        parsedChromatograms++;
      }

      // Closing tags
      xmlStreamWriter.writeEndElement(); // </chromatogramList>
      xmlStreamWriter.writeEndElement(); // </run>
      xmlStreamWriter.writeEndElement(); // </mzML>

      // <indexList>
      indexListOffset = xmlStreamWriter.getLocation().getCharacterOffsetInLong();
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_INDEX_LIST);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_COUNT, "2");

      // <index> (spectrum)
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_INDEX);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_NAME, MzMLTags.TAG_SPECTRUM);

      for (int i = 0; i < scans.size(); i++) {
        // offset
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_OFFSET);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ID_REF,
            "scan=" + scans.get(i).getScanNumber());
        xmlStreamWriter.writeCharacters(String.valueOf(spectrumIndices.get(i)));
        xmlStreamWriter.writeEndElement(); // </offset>
      }

      xmlStreamWriter.writeEndElement(); // </index>

      // <index> (chromatogram)
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_INDEX);
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_NAME, MzMLTags.TAG_CHROMATOGRAM);

      for (int i = 0; i < chromatograms.size(); i++) {
        // offset
        xmlStreamWriter.writeStartElement(MzMLTags.TAG_OFFSET);
        xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ID_REF,
            chromatograms.get(i).getChromatogramType().name());
        xmlStreamWriter.writeCharacters(String.valueOf(chromatogramIndices.get(i)));
        xmlStreamWriter.writeEndElement(); // </offset>
      }

      xmlStreamWriter.writeEndElement(); // </index>
      xmlStreamWriter.writeEndElement(); // </indexList>

      // <indexListOffset>
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_INDEX_LIST_OFFSET);
      xmlStreamWriter.writeCharacters(String.valueOf(indexListOffset));
      xmlStreamWriter.writeEndElement(); // </indexListOffset>

      // <fileChecksum>
      dos.on(false);
      String sha1Checksum = DigestUtils.shaHex(sha1.digest());
      xmlStreamWriter.writeStartElement(MzMLTags.TAG_FILE_CHECKSUM);
      xmlStreamWriter.writeCharacters(sha1Checksum);
      xmlStreamWriter.writeEndElement(); // </fileChecksum>

      xmlStreamWriter.writeEndElement(); // </indexedmzML>

      // Wrapping up
      xmlStreamWriter.writeEndDocument();
      xmlStreamWriter.close();

      setStatus(TaskStatus.FINISHED);

      logger.info("Finished export of file " + dataFile + " to " + outFilename);

    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error in file export: " + e.getMessage());
    }

  }


  /**
   * <p>
   * Write a <code>&lt;cvParam&gt;</code> to the <code>xmlStreamWriter</code>
   * </p>
   *
   * @param xmlStreamWriter an {@link javolution.xml.internal.stream.XMLStreamWriterImpl
   *                        XMLStreamWriterImpl} instance
   * @param cvParam         the CV Parameter to be written to the outFilename {@link File File}
   * @throws XMLStreamException
   */
  private void writeCVParam(XMLStreamWriterImpl xmlStreamWriter, MzMLCVParam cvParam)
      throws XMLStreamException {

    // <cvParam>
    xmlStreamWriter.writeStartElement(MzMLTags.TAG_CV_PARAM);

    // cvRef="MS"
    xmlStreamWriter.writeAttribute(MzMLTags.ATTR_CV_REF, CV_REF_MS);

    // accession="..."
    xmlStreamWriter.writeAttribute(MzMLTags.ATTR_ACCESSION, cvParam.getAccession());

    // Get optional CV param attribute such as value, name and unitAccession and write if they are
    // present
    Optional<String> value = cvParam.getValue(), name = cvParam.getName(),
        unitAccession = cvParam.getUnitAccession();

    if (name.isPresent()) {
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_NAME, name.get());
    }

    xmlStreamWriter.writeAttribute(MzMLTags.ATTR_VALUE, value.orElse(""));

    if (unitAccession.isPresent()) {
      xmlStreamWriter.writeAttribute(MzMLTags.ATTR_UNIT_ACCESSION, unitAccession.get());
    }

    xmlStreamWriter.writeEndElement(); // </cvParam>

  }

  /**
   * <p>
   * Write a group (or list) of <code>&lt;cvParam&gt;</code> to the <code>xmlStreamWriter</code>
   * </p>
   *
   * @param xmlStreamWriter an {@link javolution.xml.internal.stream.XMLStreamWriterImpl
   *                        XMLStreamWriterImpl} instance
   * @param cvGroup         the list (or group) of CV Parameters to be written to the outFilename
   *                        {@link File File}
   * @throws XMLStreamException
   */
  private void writeCVGroup(XMLStreamWriterImpl xmlStreamWriter, MzMLCVGroup cvGroup)
      throws XMLStreamException {
    for (MzMLCVParam cvParam : cvGroup.getCVParamsList()) {
      writeCVParam(xmlStreamWriter, cvParam);
    }
  }
}
