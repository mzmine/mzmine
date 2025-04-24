package io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.FoundAdduct;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.controlsfx.control.spreadsheet.SpreadsheetCellType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the column of adducts found for the feature in Lipid Validation
 */
public class LipidValidationAdductsType extends StringType implements EditableColumnType, StringParser<String>, AnnotationType  {

    private StringConverter<String> converter = new DefaultStringConverter();
    @Override
    public @NotNull String getUniqueID() {
        return "found_adducts";
    }

    @Override
    public @NotNull String getHeaderString() {
        return "Found adducts";
    }

    @Override
    public String fromString(String s) {
        return s;
    }

    @Override
    public StringConverter<String> getStringConverter() {
        return converter;
    }
}
