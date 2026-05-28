package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRowID;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMembers;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.abstr.ModularSubColumnsType;
import io.github.mzmine.util.io.JsonUtils;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aggregator carrier for compound membership state. Sub-columns: preferred row id, confidence,
 * per-member role (display-only, supplied by cell factory), and the full members list. The members
 * list sub-column persists ids + roles + scores per element; on load the row references are
 * resolved against the {@link ModularFeatureList} passed to {@link #loadFromXML}.
 */
public class CompoundMembersType extends ModularSubColumnsType<CompoundMembers> {

  private static final Logger logger = Logger.getLogger(CompoundMembersType.class.getName());

  @Override
  public @NotNull String getUniqueID() {
    return "compound_members";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Members";
  }

  @Override
  @SuppressWarnings({"rawtypes"})
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(DataTypes.get(CompoundPreferredRowType.class),
        DataTypes.get(CompoundConfidenceType.class), DataTypes.get(CompoundMemberListType.class));
  }

  @Override
  protected CompoundMembers createRecord(final SimpleModularDataModel model) {
    // unused: loadFromXML is overridden directly because preferred-row resolution requires the
    // flist parameter, which is not available here. Kept as a safe stub.
    return null;
  }

  @Override
  public Class<CompoundMembers> getValueClass() {
    return CompoundMembers.class;
  }

  @Override
  public Property<CompoundMembers> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final CompoundMembers value,
      final boolean export) {
    if (value == null) {
      return "";
    }
    // serialize as JSON: preferred row (flat id), members (flat id + role + score), confidence
    final List<CompoundMemberDTO> memberJsons = value.members().stream()
        .map(m -> new CompoundMemberDTO(FeatureListRowID.of(m.row()))).toList();
    return JsonUtils.writeStringOrEmpty(
        new CompoundMembersDTO(FeatureListRowID.of(value.preferredRow()), memberJsons));
  }

  @Override
  public Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {
    final SimpleModularDataModel model = super.loadSubColumnsFromXML(reader, project, flist, row,
        feature, file);
    if (model.isEmpty()) {
      return null;
    }
    final ModularFeatureListRow preferred = model.get(CompoundPreferredRowType.class);
    final List<CompoundFeatureMember> members = model.get(CompoundMemberListType.class);
    final Float confidence = model.get(CompoundConfidenceType.class);
    if (preferred == null || members == null) {
      logger.log(Level.WARNING,
          "Cannot reconstruct CompoundMembers — missing preferred row or members list");
      return null;
    }
    return new CompoundMembers(preferred, members, confidence != null ? confidence : 0f);
  }
}
