package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.main.MZmineCore;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class CompoundConfidenceType extends FloatType {

  public CompoundConfidenceType() {
    super(new DecimalFormat("0.000"));
  }

  @Override
  public @NotNull String getUniqueID() {
    return "compound_confidence";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Confidence";
  }

  @Override
  public NumberFormat getFormat() {
    try {
      return MZmineCore.getConfiguration().getGuiFormats().scoreFormat();
    } catch (NullPointerException e) {
      return DEFAULT_FORMAT;
    }
  }

  @Override
  public NumberFormat getExportFormat() {
    try {
      return MZmineCore.getConfiguration().getExportFormats().scoreFormat();
    } catch (NullPointerException e) {
      return DEFAULT_FORMAT;
    }
  }
}
