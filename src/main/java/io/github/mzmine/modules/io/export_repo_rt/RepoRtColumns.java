/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.export_repo_rt;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.Database;
import io.github.mzmine.datamodel.features.compoundannotations.DatabaseMatchInfo;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class RepoRtColumns {

  private final List<DBEntryField> columns = List.of(DBEntryField.ENTRY_ID, DBEntryField.NAME,
      DBEntryField.FORMULA, DBEntryField.RT, DBEntryField.PUBCHEM, DBEntryField.ISOMERIC_SMILES,
      DBEntryField.SMILES, DBEntryField.INCHI, DBEntryField.INCHIKEY, DBEntryField.CHEBI,
      DBEntryField.HMDB, DBEntryField.LIPIDMAPS, DBEntryField.KEGG, DBEntryField.COMMENT);
  private final List<String> headers = getColumns().stream().map(RepoRtColumns::getHeader).toList();

  private final NumberFormat rtFormat = new DecimalFormat("0.0000");
  private final NumberFormat idFormat = new DecimalFormat("000000");
  private final String datasetId;
  private int compoundCounter;

  public RepoRtColumns(final String datasetId, final int compoundCounterStart) {
    this.datasetId = datasetId;
    this.compoundCounter = compoundCounterStart;
  }

  public List<DBEntryField> getColumns() {
    return columns;
  }

  public List<String> getHeaders() {
    return headers;
  }

  public static String getHeader(DBEntryField field) {
    return switch (field) {
      case ENTRY_ID -> "id";
      case NAME -> "name";
      case FORMULA -> "formula";
      case RT -> "rt";
      case PUBCHEM -> "pubchem.cid";
      case SMILES -> "pubchem.smiles.canonical";
      case ISOMERIC_SMILES -> "pubchem.smiles.isomeric";
      case INCHI -> "pubchem.inchi";
      case INCHIKEY -> "pubchem.inchikey";
      case CHEBI -> "id.chebi";
      case HMDB -> "id.hmdb";
      case LIPIDMAPS -> "id.lipidmaps";
      case KEGG -> "id.kegg";
      case COMMENT -> "comment";
      default -> throw new IllegalStateException(
          "Unexpected column that is not handled in RepoRT: " + field);
    };
  }

  @Nullable
  public String getValueString(final FeatureListRow row, final FeatureAnnotation annotation,
      final DBEntryField field) {
    return switch (field) {
      case ENTRY_ID -> "%s_%s".formatted(datasetId, idFormat.format(compoundCounter++));
      case NAME -> annotation.getCompoundName();
      case FORMULA -> annotation.getFormula();
      case RT -> rtFormat.format(requireNonNullElse(row.getAverageRT(), -1f));
      case SMILES -> annotation.getSmiles();
      case ISOMERIC_SMILES -> annotation.getIsomericSmiles();
      case INCHI -> annotation.getInChI();
      case INCHIKEY -> annotation.getInChIKey();
      case PUBCHEM, CHEBI, HMDB, LIPIDMAPS, KEGG -> {
        List<DatabaseMatchInfo> dbInfos = annotation.getDatabaseMatchInfo();
        Database targetDb = Database.forField(field);
        // find entry if available
        for (final DatabaseMatchInfo info : dbInfos) {
          if (Objects.equals(info.database(), targetDb)) {
            yield info.id();
          }
        }
        yield null;
      }
      case COMMENT -> annotation.createComment();
      default -> throw new IllegalStateException(
          "Unexpected column that is not handled in RepoRT: " + field);
    };
  }
}
