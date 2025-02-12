package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.jetbrains.annotations.NotNull;

public class RIScaleType extends StringType implements AnnotationType {

    public String fromString(String s) {
        return s;
    }

    @Override
    public @NotNull String getHeaderString() {
        return "RI Scale source";
    }

    @NotNull
    @Override
    public final String getUniqueID() {
        // Never change the ID for compatibility during saving/loading of type
        return "retention_index_scale";
    }

}
