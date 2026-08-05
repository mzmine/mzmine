package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundContradiction;
import io.github.mzmine.datamodel.features.compoundlist.CompoundContradiction.ContradictionType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Holds the list of {@link CompoundContradiction}s (conflicting evidence) detected for a compound.
 * Set both on the involved member rows (so a conflict surfaces on the rows it concerns) and as a
 * roll-up on the {@link io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow}.
 * Auto-registered by the {@code DataTypes} scanner because it lives under
 * {@code io.github.mzmine.datamodel.features.types}.
 */
public class CompoundContradictionListType extends ListDataType<CompoundContradiction> {

  private static final Logger logger = Logger.getLogger(
      CompoundContradictionListType.class.getName());

  private static final String XML_ELEMENT = "contradiction";
  private static final String XML_TYPE_ATTR = "type";
  private static final String XML_SEVERITY_ATTR = "severity";
  private static final String XML_COMPOUND_ATTR = "compound";
  private static final String XML_ROWS_ATTR = "rows";
  private static final String XML_DETAIL_ATTR = "detail";

  @Override
  public @NotNull String getUniqueID() {
    return "compound_contradictions";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Contradictions";
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Class<List<CompoundContradiction>> getValueClass() {
    return (Class) List.class;
  }

  @Override
  public Property<List<CompoundContradiction>> createProperty() {
    return new SimpleObjectProperty<>(null);
  }

  @Override
  public double getPrefColumnWidth() {
    return 150;
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final List<CompoundContradiction> list,
      final boolean export) {
    if (list == null || list.isEmpty()) {
      return "";
    }
    if (list.size() == 1) {
      return list.getFirst().type().getLabel();
    }
    return list.size() + " contradictions";
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if (!(value instanceof List<?> contradictions)) {
      return;
    }
    for (final Object element : contradictions) {
      if (!(element instanceof CompoundContradiction c)) {
        continue;
      }
      writer.writeStartElement(XML_ELEMENT);
      writer.writeAttribute(XML_TYPE_ATTR, c.type().getUniqueID());
      writer.writeAttribute(XML_SEVERITY_ATTR, String.valueOf(c.severity()));
      writer.writeAttribute(XML_COMPOUND_ATTR, String.valueOf(c.compoundId()));
      writer.writeAttribute(XML_ROWS_ATTR, joinIds(c.involvedRowIds()));
      writer.writeAttribute(XML_DETAIL_ATTR, c.detail());
      writer.writeEndElement();
    }
  }

  @Override
  public Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {
    final List<CompoundContradiction> contradictions = new ArrayList<>();
    final String openingElement = reader.getLocalName();
    while (reader.hasNext()) {
      final int event = reader.next();
      if (event == XMLStreamConstants.END_ELEMENT && openingElement.equals(reader.getLocalName())) {
        break;
      }
      if (event != XMLStreamConstants.START_ELEMENT || !XML_ELEMENT.equals(reader.getLocalName())) {
        continue;
      }
      try {
        final ContradictionType type = ContradictionType.fromUniqueID(
            reader.getAttributeValue(null, XML_TYPE_ATTR));
        final float severity = Float.parseFloat(reader.getAttributeValue(null, XML_SEVERITY_ATTR));
        final int compoundId = Integer.parseInt(reader.getAttributeValue(null, XML_COMPOUND_ATTR));
        final List<Integer> rows = parseIds(reader.getAttributeValue(null, XML_ROWS_ATTR));
        final String detail = reader.getAttributeValue(null, XML_DETAIL_ATTR);
        contradictions.add(
            new CompoundContradiction(type, severity, compoundId, rows, detail == null ? "" : detail));
      } catch (final IllegalArgumentException e) {
        logger.log(Level.WARNING, "Failed to parse <contradiction> element: " + e.getMessage(), e);
      }
    }
    return contradictions.isEmpty() ? null : List.copyOf(contradictions);
  }

  private static @NotNull String joinIds(@NotNull final List<Integer> ids) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ids.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(ids.get(i));
    }
    return sb.toString();
  }

  private static @NotNull List<Integer> parseIds(@Nullable final String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    final List<Integer> ids = new ArrayList<>();
    for (final String part : Arrays.asList(csv.split(","))) {
      final String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        ids.add(Integer.parseInt(trimmed));
      }
    }
    return ids;
  }
}
