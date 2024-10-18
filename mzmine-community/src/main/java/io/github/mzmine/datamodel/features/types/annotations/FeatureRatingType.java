package io.github.mzmine.datamodel.features.types.annotations;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.abstr.SimpleSubColumnsType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;
import javafx.beans.property.Property;

public class FeatureRatingType extends DoubleType {

    public FeatureRatingType() {
        super(format);
        //TODO Auto-generated constructor stub
    }

    private static final NumberFormat format = new DecimalFormat("0.00");
    @Override
    public NumberFormat getFormat() {
        // TODO Auto-generated method stub
        return format;
    }

    @Override
    public NumberFormat getExportFormat() {
        return format;
    }

    @Override
    public @NotNull String getUniqueID() {
        return "feature_rating";
    }

    @Override
    public @NotNull String getHeaderString() {
        return "Feature rating";
    }

    @Override
    public boolean getDefaultVisibility() {
        return true;
    }
    
}
