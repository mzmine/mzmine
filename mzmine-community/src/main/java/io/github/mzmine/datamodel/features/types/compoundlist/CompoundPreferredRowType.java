package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.IgnoreAutoColumn;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.exceptions.MissingCompoundRowException;
import io.github.mzmine.util.exceptions.MissingFeatureListRowException;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sub-column under {@link CompoundMembersType} carrying the compound row's preferred (representative)
 * row reference. The value is a live {@link ModularFeatureListRow} which may be either a source row
 * from the underlying {@link ModularFeatureList} or a {@link ModularCompoundRow} (nested compound).
 * <p>
 * Persisted as {@code <datatype id="compound_preferred_row" id_ref="..." compound="true|false"/>}.
 * On load the row reference is resolved against the source feature list (when {@code
 * compound="false"}) or against the surrounding {@link CompoundList} (when {@code compound="true"}),
 * obtained via the in-construction {@link ModularCompoundRow} carried in the {@code row} parameter.
 * <p>
 * Only ever read on a {@link ModularCompoundRow} — there is no use case where a non-compound row
 * carries this type.
 */
public class CompoundPreferredRowType extends DataType<ModularFeatureListRow>
    implements IgnoreAutoColumn {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_preferred_row";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Preferred Row";
  }

  @Override
  public Class<ModularFeatureListRow> getValueClass() {
    return ModularFeatureListRow.class;
  }

  @Override
  public Property<ModularFeatureListRow> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final ModularFeatureListRow value,
      final boolean export) {
    if (value == null) {
      return "";
    }
    if (value instanceof ModularCompoundRow ccr) {
      return "compound:" + ccr.getCompoundId();
    }
    return String.valueOf(value.getID());
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if (!(value instanceof ModularFeatureListRow ref)) {
      return;
    }
    final boolean isCompound = ref instanceof ModularCompoundRow;
    final int idRef = isCompound ? ((ModularCompoundRow) ref).getCompoundId() : ref.getID();
    writer.writeAttribute(CONST.XML_COMPOUND_ID_REF_ATTR, String.valueOf(idRef));
    writer.writeAttribute(CONST.XML_COMPOUND_ATTR, String.valueOf(isCompound));
  }

  @Override
  public Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {
    final String idStr = reader.getAttributeValue(null, CONST.XML_COMPOUND_ID_REF_ATTR);
    if (idStr == null) {
      return null;
    }
    final int idRef = Integer.parseInt(idStr);
    final String compoundAttr = reader.getAttributeValue(null, CONST.XML_COMPOUND_ATTR);
    final boolean isCompound = "true".equalsIgnoreCase(compoundAttr);
    if (isCompound) {
      if (!(row instanceof ModularCompoundRow ccr)) {
        throw new IllegalStateException(
            "CompoundPreferredRowType with compound=true can only be read on a ModularCompoundRow");
      }
      final CompoundList cl = ccr.getCompoundList();
      final ModularCompoundRow resolved = cl.findRowByCompoundId(idRef);
      if (resolved == null) {
        throw new MissingCompoundRowException(
            "Cannot resolve compound preferred row reference", cl, idRef);
      }
      return resolved;
    }
    final FeatureListRow resolved = flist.findRowByID(idRef);
    if (!(resolved instanceof ModularFeatureListRow mflr)) {
      throw new MissingFeatureListRowException(
          "Cannot resolve compound preferred row reference", flist, idRef);
    }
    return mflr;
  }
}
