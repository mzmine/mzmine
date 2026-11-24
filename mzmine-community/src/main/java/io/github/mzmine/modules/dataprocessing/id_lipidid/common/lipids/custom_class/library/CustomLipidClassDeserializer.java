package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassJson;
import java.io.IOException;
import java.util.List;

/**
 * Reads a custom lipid class written by {@link CustomLipidClassSerializer}.
 */
class CustomLipidClassDeserializer extends JsonDeserializer<CustomLipidClass> {

  @Override
  public CustomLipidClass deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    final JsonNode node = p.getCodec().readTree(p);
    final List<CustomLipidClass> classes = CustomLipidClassJson.fromJson("[" + node + "]");
    if (classes.isEmpty()) {
      throw ctxt.weirdStringException(node.toString(), CustomLipidClass.class,
          "Not a custom lipid class");
    }
    return classes.getFirst();
  }
}
