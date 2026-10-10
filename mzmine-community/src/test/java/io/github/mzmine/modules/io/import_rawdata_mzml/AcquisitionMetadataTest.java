package io.github.mzmine.modules.io.import_rawdata_mzml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLAcquisitionMetadata;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import java.io.StringReader;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.junit.jupiter.api.Test;

class AcquisitionMetadataTest {
  @Test void retainsOnlyUsedInstrumentAndCanonicalTermsIncludingGroups() throws Exception {
    final var metadata = parse("""
        <mzML><referenceableParamGroupList><referenceableParamGroup id="model">
        <cvParam accession="MS:1001911" name="PRIVATE SAMPLE NAME"/>
        </referenceableParamGroup></referenceableParamGroupList>
        <instrumentConfigurationList><instrumentConfiguration id="used">
        <referenceableParamGroupRef ref="model"/><userParam name="serial number" value="PRIVATE SERIAL"/>
        <componentList><analyzer><cvParam accession="MS:1000484" name="PRIVATE TEXT"/></analyzer>
        <source><cvParam accession="MS:1000073" name="private"/></source></componentList>
        </instrumentConfiguration><instrumentConfiguration id="unused">
        <cvParam accession="MS:1003230" name="timsTOF Pro 2"/>
        </instrumentConfiguration></instrumentConfigurationList>
        <run defaultInstrumentConfigurationRef="used"><spectrum>
        <cvParam accession="MS:1003221" name="PRIVATE STUDY"/>
        <cvParam accession="MS:9999999" name="SECRET"/>
        </spectrum></run></mzML>
        """);
    assertEquals(4, metadata.terms().size());
    assertTrue(metadata.terms().stream().anyMatch(t -> t.label().equals("Q Exactive")));
    assertFalse(metadata.toString().contains("PRIVATE"));
    assertFalse(metadata.toString().contains("timsTOF"));
    assertTrue(metadata.terms().stream().anyMatch(t -> t.field() == Field.ACQUISITION_METHOD));
  }

  @Test void absentMethodIsUnknownAndNotGuessedFromMsLevels() throws Exception {
    final var metadata = parse("""
        <mzML><run defaultInstrumentConfigurationRef="missing"><spectrum>
        <cvParam accession="MS:1000511" name="ms level" value="2"/>
        <cvParam accession="MS:1000130" name="positive scan"/>
        </spectrum></run></mzML>
        """);
    assertTrue(metadata.terms().isEmpty());
  }

  @Test void acquisitionFactsStaySeparateFromSampleMetadata() {
    final RawDataFile file = mock(RawDataFile.class);
    when(file.getName()).thenReturn("private_QC.mzML");
    when(file.getAcquisitionMetadata()).thenReturn(new AcquisitionMetadata(
        AcquisitionMetadata.resolve("MS:1001911"), java.util.Map.of("SampleName", "private study")));
    when(file.getMSLevels()).thenReturn(new int[]{1,2});
    when(file.getDataPolarity()).thenReturn(List.of());
    when(file.getSpectraType()).thenReturn(MassSpectrumType.CENTROIDED);
    final MetadataTable table = new MetadataTable();
    final var existing = new StringMetadataColumn("Sample group");
    table.setValue(existing, file, "control");
    table.addFile(file);
    assertEquals("control", table.getColumnData(existing).get(file));
    assertNull(table.getColumnByName("Imported: SampleName"));
    assertNull(table.getColumnByName("Measured: MS levels"));
    assertEquals("private study", file.getAcquisitionMetadata().localFields().get("SampleName"));
  }

  @Test void brukerMissingKeysStayUnknownAndModesUseExistingDeclarations() {
    final TDFMetaDataTable table = new TDFMetaDataTable();
    final var result = table.acquisitionMetadata(List.of(9L));
    assertTrue(result.terms().stream().anyMatch(t -> t.label().equals("data-independent acquisition")));
    assertTrue(result.terms().stream().noneMatch(t -> t.field() == Field.INSTRUMENT_MODEL));
    assertTrue(AcquisitionMetadata.resolveLabel(Field.INSTRUMENT_MODEL, "timsTOF Pro 2").size() == 1);
    assertTrue(AcquisitionMetadata.resolveLabel(Field.INSTRUMENT_MODEL, "unknown sample label").isEmpty());
  }

  private static AcquisitionMetadata parse(final String xml) throws Exception {
    final var parser = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(xml));
    final var metadata = new MzMLAcquisitionMetadata();
    while (parser.hasNext()) {
      final int event = parser.next();
      if (event == XMLStreamConstants.START_ELEMENT) metadata.open(parser, parser.getLocalName());
      else if (event == XMLStreamConstants.END_ELEMENT) metadata.close(parser.getLocalName());
    }
    parser.close();
    return metadata.result();
  }
}
