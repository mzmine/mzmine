package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.modifiers.IgnoreAutoColumn;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.util.exceptions.MissingCompoundRowException;
import io.github.mzmine.util.exceptions.MissingFeatureListRowException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sub-column under {@link CompoundMembersType} carrying the actual list of compound members. Each
 * member is persisted as a {@code <member id="..." role="..." score="..."/>} element. On load, row
 * ids are resolved back to {@link ModularFeatureListRow}s via {@link
 * ModularFeatureList#findRowByID(int)}; members whose source row is missing are skipped (a logged
 * warning), accepting the dangling-reference scenario noted in the design review.
 */
public class CompoundMemberListType extends ListDataType<CompoundFeatureMember>
    implements IgnoreAutoColumn {

  private static final Logger logger = Logger.getLogger(CompoundMemberListType.class.getName());

  private static final String XML_MEMBER_ELEMENT = "member";
  private static final String XML_ID_ATTR = "id";
  private static final String XML_ROLE_ATTR = "role";
  private static final String XML_SCORE_ATTR = "score";
  private static final String XML_COMPOUND_ATTR = "compound";

  @Override
  public @NotNull String getUniqueID() {
    return "compound_member_list";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Members list";
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Class<List<CompoundFeatureMember>> getValueClass() {
    return (Class) List.class;
  }

  @Override
  public Property<List<CompoundFeatureMember>> createProperty() {
    return new SimpleObjectProperty<>(null);
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if (!(value instanceof List<?> members)) {
      return;
    }
    for (final Object element : members) {
      if (!(element instanceof CompoundFeatureMember m)) {
        continue;
      }
      writer.writeStartElement(XML_MEMBER_ELEMENT);
      final boolean isCompound = m.row() instanceof ModularCompoundRow;
      final int idRef =
          isCompound ? ((ModularCompoundRow) m.row()).getCompoundId() : m.row().getID();
      writer.writeAttribute(XML_ID_ATTR, String.valueOf(idRef));
      writer.writeAttribute(XML_COMPOUND_ATTR, String.valueOf(isCompound));
      writer.writeAttribute(XML_ROLE_ATTR, m.role().name());
      writer.writeAttribute(XML_SCORE_ATTR, String.valueOf(m.score()));
      writer.writeEndElement();
    }
  }

  @Override
  public Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {
    final List<CompoundFeatureMember> members = new ArrayList<>();
    final String openingElement = reader.getLocalName();
    while (reader.hasNext()) {
      final int event = reader.next();
      if (event == XMLStreamConstants.END_ELEMENT && openingElement.equals(reader.getLocalName())) {
        break;
      }
      if (event != XMLStreamConstants.START_ELEMENT) {
        continue;
      }
      if (!XML_MEMBER_ELEMENT.equals(reader.getLocalName())) {
        continue;
      }
      final String idStr = reader.getAttributeValue(null, XML_ID_ATTR);
      final String roleStr = reader.getAttributeValue(null, XML_ROLE_ATTR);
      final String scoreStr = reader.getAttributeValue(null, XML_SCORE_ATTR);
      final String compoundStr = reader.getAttributeValue(null, XML_COMPOUND_ATTR);
      if (idStr == null || roleStr == null || scoreStr == null) {
        logger.log(Level.WARNING, "Skipping <member> element with missing attributes");
        continue;
      }
      try {
        final int id = Integer.parseInt(idStr);
        final boolean isCompound = "true".equalsIgnoreCase(compoundStr);
        final ModularFeatureListRow member = isCompound
            ? resolveCompound(row, id)
            : resolveSource(flist, id);
        final CompoundMemberRole role = CompoundMemberRole.valueOf(roleStr);
        final float score = Float.parseFloat(scoreStr);
        members.add(new CompoundFeatureMember(member, role, score));
      } catch (IllegalArgumentException e) {
        logger.log(Level.WARNING, "Failed to parse <member> element: " + e.getMessage(), e);
      }
    }
    return members.isEmpty() ? null : List.copyOf(members);
  }

  private static @NotNull ModularFeatureListRow resolveSource(
      @NotNull final ModularFeatureList flist, final int id) {
    final FeatureListRow resolved = flist.findRowByID(id);
    if (!(resolved instanceof ModularFeatureListRow mflr)) {
      throw new MissingFeatureListRowException(
          "Compound member row not found. Cannot construct compound row.", flist, id);
    }
    return mflr;
  }

  private static @NotNull ModularFeatureListRow resolveCompound(
      @NotNull final ModularFeatureListRow row, final int compoundId) {
    if (!(row instanceof ModularCompoundRow ccr)) {
      throw new IllegalStateException(
          "CompoundMemberListType compound member can only be read on a ModularCompoundRow");
    }
    final CompoundList cl = ccr.getCompoundList();
    final ModularCompoundRow resolved = cl.findRowByCompoundId(compoundId);
    if (resolved == null) {
      throw new MissingCompoundRowException(
          "Compound member compound row not found", cl, compoundId);
    }
    return resolved;
  }
}
