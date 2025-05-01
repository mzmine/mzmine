package io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge;

import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents the column of positive evidence found for the feature in Lipid Validation module.
 * It will be displayed in the feature list table in the GUI under the name "Correct comments".
 */
public class LipidValidationCorrectDescriptionType extends StringType implements EditableColumnType, StringParser<String>, AnnotationType {
    /**
     * Converter to pass the objects.
     */
    private StringConverter<String> converter = new DefaultStringConverter();
    /**
     * Header (name) for the column.
     * It is what appears at the top of the column in the feature list table.
     * @return The column header.
     */
    @Override
    public @NotNull String getHeaderString() {
        return "Correct comments";
    }
    /**
     * Returns the same parameter it was given as input.
     * @param s Input String parameter.
     * @return The input parameter.
     */
    @Override
    public String fromString(String s) {
        return s;
    }
    /**
     * Gets the converter object.
     * @return The converter parameter.
     */
    @Override
    public StringConverter<String> getStringConverter() {
        return converter;
    }

    /**
     * Identifier for the column.
     * @return The column ID.
     */
    @NotNull
    @Override
    public final String getUniqueID() {
        // Never change the ID for compatibility during saving/loading of type
        return "comment_correct";
    }
}