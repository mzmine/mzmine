package io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the column of score assigned for the feature in Lipid Validation
 */
public class LipidValidationScoreType extends ScoreType {

    @NotNull
    @Override
    public final String getUniqueID() {
        // Never change the ID for compatibility during saving/loading of type
        return "lipidExpertKnowledge_score";
    }

    @Override
    public @NotNull String getHeaderString() {
        return "Score";
    }

    @Override
    public boolean getDefaultVisibility() {
        return true; // inherits from score type, but is not as important
    }
}
