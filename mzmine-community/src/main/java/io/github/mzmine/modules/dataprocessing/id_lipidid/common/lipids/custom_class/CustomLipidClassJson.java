package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Reads and writes custom lipid classes in the JSON format of the import and export buttons. The
 * format is a JSON list of classes. Unknown fields, e.g., additional metadata,
 * are ignored when reading.
 */
public final class CustomLipidClassJson {

  public static final String FILE_EXTENSION = "*.json";

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
      .create();
  private static final Type LIST_TYPE = new TypeToken<List<CustomLipidClass>>() {
  }.getType();

  private CustomLipidClassJson() {
  }

  /**
   * @param json a JSON list of lipid classes
   * @return the classes, never containing null
   * @throws JsonParseException if the JSON is invalid
   */
  public static @NotNull List<CustomLipidClass> fromJson(@NotNull String json) {
    final List<CustomLipidClass> classes = GSON.fromJson(json, LIST_TYPE);
    return classes == null ? List.of() : classes.stream().filter(Objects::nonNull).toList();
  }

  /**
   * @param reader reads a JSON list of lipid classes
   * @return the classes, never containing null
   * @throws JsonParseException if the JSON is invalid
   */
  public static @NotNull List<CustomLipidClass> fromJson(@NotNull Reader reader) {
    final List<CustomLipidClass> classes = GSON.fromJson(reader, LIST_TYPE);
    return classes == null ? List.of() : classes.stream().filter(Objects::nonNull).toList();
  }

  /**
   * @param file a JSON file exported by mzmine
   * @return the classes, never containing null
   * @throws IOException        if the file cannot be read
   * @throws JsonParseException if the JSON is invalid
   */
  public static @NotNull List<CustomLipidClass> read(@NotNull Path file) throws IOException {
    try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return fromJson(reader);
    }
  }

  /**
   * @param classes the classes to write as JSON list
   * @return pretty printed JSON
   */
  public static @NotNull String toJson(@NotNull Collection<? extends ILipidClass> classes) {
    return GSON.toJson(classes.stream().map(CustomLipidClassJson::toCustom).toList());
  }

  /**
   * @param file    the target file, is overwritten
   * @param classes the classes to write
   * @throws IOException if the file cannot be written
   */
  public static void write(@NotNull Path file, @NotNull Collection<? extends ILipidClass> classes)
      throws IOException {
    Files.writeString(file, toJson(classes), StandardCharsets.UTF_8);
  }

  /**
   * Built-in classes are enums and would be written as their name, so they are copied into a
   * custom class first.
   *
   * @param lipidClass any lipid class
   * @return a custom lipid class with the same content
   */
  public static @NotNull CustomLipidClass toCustom(@NotNull ILipidClass lipidClass) {
    if (lipidClass instanceof CustomLipidClass custom) {
      return custom;
    }
    return new CustomLipidClass(lipidClass.getName(), lipidClass.getAbbr(),
        lipidClass.getCoreClass(), lipidClass.getMainClass(), lipidClass.getBackBoneFormula(),
        lipidClass.getChainTypes(), lipidClass.getFragmentationRules());
  }

  /**
   * @return all built-in classes as custom classes, e.g., to use as templates
   */
  public static @NotNull List<CustomLipidClass> builtInClasses() {
    return Arrays.stream(LipidClasses.values()).map(CustomLipidClassJson::toCustom)
        .toList();
  }
}
