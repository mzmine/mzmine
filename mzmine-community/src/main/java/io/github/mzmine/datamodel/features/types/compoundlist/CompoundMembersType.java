package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.IgnoreAutoColumn;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the list of {@link CompoundFeatureMember}s for a compound row. Excluded from
 * auto-column rendering via {@link IgnoreAutoColumn}; contents are exposed via tree expansion.
 * Full XML round-trip is implemented in Sprint 4.
 */
@SuppressWarnings("unchecked")
public class CompoundMembersType extends DataType<List<CompoundFeatureMember>>
    implements IgnoreAutoColumn {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_members";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Members";
  }

  @Override
  public Class<List<CompoundFeatureMember>> getValueClass() {
    return (Class) List.class;
  }

  @Override
  public Property<List<CompoundFeatureMember>> createProperty() {
    return new SimpleObjectProperty<>(null);
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final List<CompoundFeatureMember> value,
      final boolean export) {
    if (value == null) {
      return "";
    }
    return String.valueOf(value.size());
  }
}
