package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassJson;
import io.github.mzmine.util.io.JsonUtils;
import java.io.IOException;
import java.util.List;

/**
 * Writes a custom lipid class with the same JSON format as the import and export of mzmine.
 */
class CustomLipidClassSerializer extends JsonSerializer<CustomLipidClass> {

  @Override
  public void serialize(CustomLipidClass value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    final String json = CustomLipidClassJson.toJson(List.of(value));
    // decision: the export format is a list, the preset stores the single class object
    gen.writeTree(JsonUtils.MAPPER.readTree(json).get(0));
  }
}
