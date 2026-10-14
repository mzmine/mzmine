/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.DetectorsType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentSerialNumberType;
import io.github.mzmine.datamodel.features.rawfiletypes.IonSourcesType;
import io.github.mzmine.datamodel.features.rawfiletypes.MassAnalyzersType;
import io.github.mzmine.datamodel.features.rawfiletypes.RawFileMetadataTypes;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleNameType;
import io.github.mzmine.datamodel.features.rawfiletypes.SourceFileSha1Type;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLTags;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.header.MzMLFileMetadataMapper;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.header.MzMLHeaderMetadata;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.TagTracker;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Parses only the mzML header, the same way as {@link MSDKmzMLImportTask}, and maps it to the raw
 * file metadata.
 */
class MzMLFileMetadataTest {

  /**
   * Streams the header until the run element, like MzMLParser does for the header.
   */
  private static @NotNull ModularDataModel readHeader(@NotNull final InputStream is)
      throws Exception {
    final MzMLHeaderMetadata header = new MzMLHeaderMetadata();
    final TagTracker tracker = new TagTracker();
    final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
    try {
      while (reader.hasNext()) {
        final int event = reader.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          final String tag = reader.getLocalName();
          tracker.enter(tag);
          if (MzMLTags.TAG_RUN.equals(tag)) {
            header.setRunReferences(
                reader.getAttributeValue(null, MzMLTags.ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF),
                reader.getAttributeValue(null, MzMLTags.ATTR_DEFAULT_SOURCE_FILE_REF),
                reader.getAttributeValue(null, MzMLTags.ATTR_SAMPLE_REF));
            break;
          }
          header.processOpeningTag(tracker, reader, tag);
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          final String tag = reader.getLocalName();
          tracker.exit(tag);
          header.processClosingTag(tag);
        }
      }
    } finally {
      reader.close();
    }
    final SimpleModularDataModel metadata = new SimpleModularDataModel();
    MzMLFileMetadataMapper.apply(header, metadata);
    return metadata;
  }

  private static @NotNull ModularDataModel readResource(@NotNull final String resource)
      throws Exception {
    try (InputStream is = Objects.requireNonNull(
        MzMLFileMetadataTest.class.getClassLoader().getResourceAsStream(resource), resource)) {
      return readHeader(is);
    }
  }

  @Test
  void typesAreRegistered() {
    Assertions.assertNotNull(DataTypes.get(InstrumentModelType.class));
    Assertions.assertNotNull(DataTypes.getTypeForId(new InstrumentModelType().getUniqueID()));
    Assertions.assertEquals(new InstrumentSerialNumberType(),
        RawFileMetadataTypes.forAccession("MS:1000529"));
    Assertions.assertEquals(new SampleNameType(), RawFileMetadataTypes.forAccession("MS:1000002"));
  }

  @Test
  void thermoOrbitrapHeader() throws Exception {
    final ModularDataModel metadata = readResource("rawdatafiles/additional/gc_orbi_a.mzML");

    Assertions.assertEquals("Exactive", metadata.get(InstrumentModelType.class));
    Assertions.assertEquals("Exactive Series slot #33",
        metadata.get(InstrumentSerialNumberType.class));
    Assertions.assertEquals(List.of("chemical ionization"), metadata.get(IonSourcesType.class));
    Assertions.assertEquals(List.of("orbitrap"), metadata.get(MassAnalyzersType.class));
    Assertions.assertEquals(List.of("inductive detector"), metadata.get(DetectorsType.class));
    Assertions.assertEquals("Xcalibur", metadata.get(AcquisitionSoftwareType.class));
    Assertions.assertEquals("2.9-290204/2.9.2.2947",
        metadata.get(AcquisitionSoftwareVersionType.class));
    Assertions.assertEquals("N02", metadata.get(SampleNameType.class));
    Assertions.assertEquals("a7bf52e6b4cc2739fb0b692fef6a27bb90c4f2ac",
        metadata.get(SourceFileSha1Type.class));
  }

  @Test
  void brukerTimsHeader() throws Exception {
    final ModularDataModel metadata = readResource(
        "rawdatafiles/additional/lc-tims-ms-pasef-a_2frames.mzML");

    // only the generic vendor term is available
    Assertions.assertEquals("Bruker Daltonics instrument model",
        metadata.get(InstrumentModelType.class));
    Assertions.assertEquals("1854399.00052", metadata.get(InstrumentSerialNumberType.class));
    Assertions.assertEquals(List.of("electrospray ionization", "electrospray inlet"),
        metadata.get(IonSourcesType.class));
    Assertions.assertEquals(List.of("quadrupole", "time-of-flight"),
        metadata.get(MassAnalyzersType.class));
    Assertions.assertEquals(List.of("microchannel plate detector", "photomultiplier"),
        metadata.get(DetectorsType.class));
    Assertions.assertEquals("Compass", metadata.get(AcquisitionSoftwareType.class));
    Assertions.assertEquals("5.0.9", metadata.get(AcquisitionSoftwareVersionType.class));
    // default source file of the run
    Assertions.assertEquals("a5fde24f9b9bf0599b14e13e1bf41eb43e46f54e",
        metadata.get(SourceFileSha1Type.class));
    Assertions.assertNull(metadata.get(SampleNameType.class));
  }

  @Test
  void userParamModelBeatsGenericTerm() throws Exception {
    // shortened Agilent header converted by msconvert
    final String mzml = """
        <mzML xmlns="http://psi.hupo.org/ms/mzml">
          <referenceableParamGroupList count="1">
            <referenceableParamGroup id="CommonInstrumentParams">
              <cvParam cvRef="MS" accession="MS:1000490" name="Agilent instrument model" value=""/>
              <cvParam cvRef="MS" accession="MS:1000529" name="instrument serial number" value="SG1201B001"/>
              <userParam name="instrument model" value="QTOF"/>
            </referenceableParamGroup>
          </referenceableParamGroupList>
          <instrumentConfigurationList count="1">
            <instrumentConfiguration id="IC1">
              <referenceableParamGroupRef ref="CommonInstrumentParams"/>
            </instrumentConfiguration>
          </instrumentConfigurationList>
          <run id="a" defaultInstrumentConfigurationRef="IC1">
          </run>
        </mzML>
        """;
    final ModularDataModel metadata = readHeader(
        new ByteArrayInputStream(mzml.getBytes(StandardCharsets.UTF_8)));

    Assertions.assertEquals("QTOF", metadata.get(InstrumentModelType.class));
    Assertions.assertEquals("SG1201B001", metadata.get(InstrumentSerialNumberType.class));
    Assertions.assertNull(metadata.get(MassAnalyzersType.class));
    Assertions.assertNull(metadata.get(AcquisitionSoftwareType.class));
  }
}
