package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_bridge;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public enum SiriusDatabaseType {
  ALL_SIRIUS("BIO,METACYC,CHEBI,COCONUT,GNPS,HMDB,HSDB,KEGG,KNAPSACK,MACONDA,MESH,NORMAN,UNDP,"
      + "PLANTCYC,PUBCHEM,PUBMED,YMDB,ZINCBIO"),
  ONLY_COMPOUNDS(""),
  COMBINED("BIO,METACYC,CHEBI,COCONUT,ECOCYCMINE,GNPS,HMDB,HSDB,KEGG,KEGGMINE,KNAPSACK,MACONDA,"
      + "MESH,NORMAN,UNDP,PLANTCYC,PUBCHEM,PUBMED,YMDB,YMDBMINE,ZINCBIO"),
  PUBCHEM("PUBCHEM");

  final String dbstring;

  SiriusDatabaseType(String str) {
    this.dbstring = str;
  }

  @NotNull
  public String getDbString(final File[] customDbs) {
    if(customDbs == null) {
      return "";
    }
    final List<@NotNull File> files = Arrays.stream(customDbs).filter(Objects::nonNull).toList();
    if(files.isEmpty()) {
      return "";
    }

    if(this == ONLY_COMPOUNDS && customDbs != null) {
      return getFilesString(customDbs);
    } else if(this == COMBINED && customDbs != null) {
      return dbstring + "," + getFilesString(customDbs);
    } else {
      return dbstring;
    }
  }

  @NotNull
  private String getFilesString(File[] customDbs) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < customDbs.length; i++) {
      File customDb = customDbs[i];
      sb.append("\"");
      sb.append(customDb.getAbsolutePath());
      sb.append("\"");
      if(i < customDbs.length - 1) {
        sb.append(",");
      }
    }
    return sb.toString();
  }
}
