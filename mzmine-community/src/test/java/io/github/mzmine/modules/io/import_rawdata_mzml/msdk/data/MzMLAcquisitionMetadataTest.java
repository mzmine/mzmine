package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import java.io.StringReader;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.junit.jupiter.api.Test;

class MzMLAcquisitionMetadataTest {

  @Test
  void keepsHeaderDeclarationsAndExcludesSpectrumValues() throws Exception {
    final var reader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("""
        <mzML><instrumentConfiguration id="used"><cvParam accession="MS:1001911" name="Q Exactive"/>
        <componentList><source><cvParam accession="MS:1000073" name="electrospray ionization"/>
        </source></componentList></instrumentConfiguration><run defaultInstrumentConfigurationRef="used"><spectrum>
        <cvParam accession="MS:1000511" name="ms level"/></spectrum></run></mzML>
        """));
    final var metadata = new MzMLAcquisitionMetadata();
    while (reader.hasNext()) {
      final int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        metadata.open(reader, reader.getLocalName());
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        metadata.close(reader.getLocalName());
      }
    }

    final var terms = metadata.result().terms();
    assertEquals(2, terms.size());
    assertTrue(terms.stream().anyMatch(term -> term.field() == Field.INSTRUMENT_MODEL));
    assertTrue(terms.stream().anyMatch(term -> term.field() == Field.IONIZATION));
  }

  @Test
  void parserRetainsUsedInstrumentAfterClosingAnEarlierComponentList() throws Exception {
    final MzMLParser parser = new MzMLParser(mock(MSDKmzMLImportTask.class), null,
        ScanImportProcessorConfig.createDefault());
    final var reader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("""
        <mzML><instrumentConfigurationList count="2">
        <instrumentConfiguration id="unused"><componentList count="1"><source order="1">
        <cvParam accession="MS:1000073" name="electrospray ionization"/>
        </source></componentList></instrumentConfiguration>
        <instrumentConfiguration id="used"><cvParam accession="MS:1001911" name="Q Exactive"/>
        </instrumentConfiguration></instrumentConfigurationList>
        <run id="run" defaultInstrumentConfigurationRef="used" startTimeStamp="2026-09-28T12:00:00">
        <spectrumList count="0" defaultDataProcessingRef="processing"/></run></mzML>
        """));
    try {
      while (reader.hasNext()) {
        final int event = reader.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          parser.processOpeningTag(reader, reader.getLocalName());
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          parser.processClosingTag(reader, reader.getLocalName());
        }
      }
    } finally {
      reader.close();
    }

    // Each XML element must reach the collector once so sibling instrument scopes stay separate.
    assertEquals(AcquisitionMetadata.resolve("MS:1001911"),
        parser.getMzMLRawFile().getAcquisitionMetadata().terms());
  }

  @Test
  void rejectsArbitraryTermsFromTheControlledTermChannel() {
    final AcquisitionMetadata metadata = new AcquisitionMetadata(List.of(
        new AcquisitionMetadata.Term(Field.INSTRUMENT_MODEL, "MS:9999999", "vendor private")));
    assertTrue(metadata.terms().isEmpty());
  }
}
