package io.github.mzmine.datamodel.features.types.annotations.compounddb;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.main.MZmineCore;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PubChemIdType extends StringType {

  private static final Logger logger = Logger.getLogger(PubChemIdType.class.getName());

  public PubChemIdType() {
  }

  @Override
  public @NotNull String getUniqueID() {
    return "pubchem_cid";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "PubChemCID";
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file) {
    final List<CompoundDBAnnotation> compoundAnnotations = row.getCompoundAnnotations();
    if (compoundAnnotations.isEmpty()) {
      return null;
    }

    final String pubchemId = compoundAnnotations.get(0).get(PubChemIdType.class);
    if (pubchemId == null || pubchemId.isBlank()) {
      return null;
    }

    try {
      final URL url = new URL("https://pubchem.ncbi.nlm.nih.gov/compound/" + pubchemId);
      return () -> {
        MZmineCore.getDesktop().openWebPage(url);
      };
    } catch (MalformedURLException e) {
      logger.log(Level.WARNING, "Cannot open URL for compound ID " + pubchemId);
      return null;
    }
  }
}
