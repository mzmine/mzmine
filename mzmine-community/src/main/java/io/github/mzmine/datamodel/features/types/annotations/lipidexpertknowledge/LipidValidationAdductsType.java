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
 * This class represents the column of adducts found for the feature in Lipid Validation module.
 * It will be displayed in the feature list table in the GUI under the name "Found adducts".
 */
public class LipidValidationAdductsType extends StringType implements EditableColumnType, StringParser<String>, AnnotationType  {

    /**
     * Converter to pass the objects.
     */
    private StringConverter<String> converter = new DefaultStringConverter();

    /**
     * Identifier for the column.
     * @return The column ID.
     */
    @Override
    public @NotNull String getUniqueID() {
        return "found_adducts";
    }

    /**
     * Header (name) for the column.
     * It is what appears at the top of the column in the feature list table.
     * @return The column header.
     */
    @Override
    public @NotNull String getHeaderString() {
        return "Found adducts";
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
}
