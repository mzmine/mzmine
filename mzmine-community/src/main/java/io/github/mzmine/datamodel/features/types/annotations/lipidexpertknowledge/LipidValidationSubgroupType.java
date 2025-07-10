package io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents the column of subgroup assigned for the feature in Lipid Validation module.
 * It will be displayed in the feature list table in the GUI under the name "Subgroup ID".
 */
public class LipidValidationSubgroupType extends ScoreType {

    /**
     * Identifier for the column.
     * @return The column ID.
     */
    @NotNull
    @Override
    public final String getUniqueID() {
        // Never change the ID for compatibility during saving/loading of type
        return "lipidExpertKnowledge_subgroup";
    }
    /**
     * Header (name) for the column.
     * It is what appears at the top of the column in the feature list table.
     * @return The column header.
     */
    @Override
    public @NotNull String getHeaderString() {
        return "Subgroup ID";
    }

    /**
     * Sets the visibility of this column.
     * True = appears by default, False = does not appear by default.
     * @return True value.
     */
    @Override
    public boolean getDefaultVisibility() {
        return true; // inherits from score type, but is not as important
    }
}
