package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.datamodel.AcquisitionMetadata;
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
  void rejectsArbitraryTermsFromTheControlledTermChannel() {
    final AcquisitionMetadata metadata = new AcquisitionMetadata(List.of(
        new AcquisitionMetadata.Term(Field.INSTRUMENT_MODEL, "MS:9999999", "vendor private")));
    assertTrue(metadata.terms().isEmpty());
  }
}
