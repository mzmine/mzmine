package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.types.abstr.StringType;
import org.jetbrains.annotations.NotNull;

/** Display-only role column for member rows. Value is supplied by cell factory, not stored. */
public class CompoundMemberRoleType extends StringType {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_member_role";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Role";
  }
}
