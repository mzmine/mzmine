package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.time.LocalDateTime;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class FeatureRowNormalizationFunctionTest {

  @Test
  void packedXmlPreservesExactFactorsAndReasonsInOnePayload() throws Exception {
    final FeatureRowNormalizationFunction function = new FeatureRowNormalizationFunction(
        Map.of(9, Double.longBitsToDouble(0x3ff0000000000001L), 4, 2.5d),
        Map.of(3, "insufficient QC", 8, "insufficient QC", 12, "missing measurement"));
    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement("function");
    document.appendChild(element);

    function.saveToXML(element);

    assertEquals(1, XMLUtils.streamChildElementsByTagName(element, "packedRowFactors").count());
    assertEquals(function, FeatureRowNormalizationFunction.loadFromXML(element));
    assertEquals(Double.doubleToRawLongBits(function.factors().get(9)),
        Double.doubleToRawLongBits(FeatureRowNormalizationFunction.loadFromXML(element).factors().get(9)));
  }

  @Test
  void rejectsMissingOrDuplicatePackedPayloads() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement("function");
    document.appendChild(element);
    assertThrows(IllegalArgumentException.class,
        () -> FeatureRowNormalizationFunction.loadFromXML(element));

    final FeatureRowNormalizationFunction function = new FeatureRowNormalizationFunction(
        Map.of(7, 1.25d), Map.of(8, "missing QC"));
    function.saveToXML(element);
    element.appendChild(element.getFirstChild().cloneNode(true));
    assertThrows(IllegalArgumentException.class,
        () -> FeatureRowNormalizationFunction.loadFromXML(element));
  }

  @Test
  void rejectsOverlappingFactorsAndUnavailableReasons() {
    assertThrows(IllegalArgumentException.class,
        () -> new FeatureRowNormalizationFunction(Map.of(7, 1.25d), Map.of(7, "missing QC")));
  }

  @Test
  void rejectsPackedCountsThatCannotFitInTheDecodedPayload() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element function = document.createElement("function");
    document.appendChild(function);
    final Element payload = document.createElement("packedRowFactors");
    payload.setAttribute("version", "1");
    payload.setAttribute("encoding", "base64-binary");
    payload.setAttribute("factorCount", "0");
    payload.setAttribute("unavailableCount", "2147483647");
    payload.setTextContent(Base64.getEncoder().encodeToString(ByteBuffer.allocate(8)
        .putInt(0).putInt(Integer.MAX_VALUE).array()));
    function.appendChild(payload);

    assertThrows(IllegalArgumentException.class,
        () -> FeatureRowNormalizationFunction.loadFromXML(function));
  }

  @Test
  void rowDependenceAndProtectedIdsPropagateThroughNestedFunctions() {
    final FeatureRowNormalizationFunction rowFunction = new FeatureRowNormalizationFunction(
        Map.of(5, 2d), Map.of(7, "missing QC"));
    final NormalizationFunction nested = new InterpolatedNormalizationFunction(
        new CompositeNormalizationFunction(List.of(new FactorNormalizationFunction(1d), rowFunction)),
        .5d, new FactorNormalizationFunction(1d), .5d);

    assertTrue(nested.isRowDependent());
    assertEquals(Set.of(5, 7), nested.protectedRowIds());
    assertFalse(new FactorNormalizationFunction(1d).isRowDependent());
  }

  @Test
  void saveLoadThroughAppliedSummaryRetainsPartialAvailability() throws Exception {
    final FeatureRowNormalizationFunction function = new FeatureRowNormalizationFunction(
        Map.of(1, 1.5d), Map.of(2, "missing QC"));
    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.setValue(new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(
        new RawDataFilePlaceholder("sample", null), LocalDateTime.of(2026, 1, 1, 0, 0),
        function))));

    final String xml = ParameterUtils.saveParameterToXMLString(parameter);
    final NormalizationFunctionsParameter loaded = new NormalizationFunctionsParameter();
    ParameterUtils.loadParameterFromString(loaded, xml);

    assertEquals(function, loaded.getValue().getRawFileFunction(0).function());
  }
}
