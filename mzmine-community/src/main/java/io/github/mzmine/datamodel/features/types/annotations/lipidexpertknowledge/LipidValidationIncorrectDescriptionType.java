package io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge;

import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.jetbrains.annotations.NotNull;

public class LipidValidationIncorrectDescriptionType extends StringType implements EditableColumnType, StringParser<String>, AnnotationType {

    private StringConverter<String> converter = new DefaultStringConverter();

    @Override
    public @NotNull String getHeaderString() {
        return "Incorrect comments";
    }

    @Override
    public String fromString(String s) {
        return s;
    }

    @Override
    public StringConverter<String> getStringConverter() {
        return converter;
    }

    @NotNull
    @Override
    public final String getUniqueID() {
        // Never change the ID for compatibility during saving/loading of type
        return "comment_incorrect";
    }
}