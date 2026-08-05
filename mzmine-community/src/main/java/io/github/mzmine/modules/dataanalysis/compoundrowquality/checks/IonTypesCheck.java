/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/// Lists the distinct ion types observed across the compound's non-isotope members.
public final class IonTypesCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.ION_TYPES;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    // First-seen representative row per distinct ion-type label; insertion order preserved so the
    // chips render in the same order ions first appeared on the compound.
    final Map<String, FeatureListRow> distinct = new LinkedHashMap<>();
    final List<FeatureListRow> involved = new ArrayList<>();

    for (final CompoundFeatureMember member : members) {
      if (member.role() == CompoundMemberRole.ISOTOPOLOGUE) {
        continue;
      }
      final IonIdentity ion = member.row().getBestIonIdentity();
      if (ion == null) {
        continue;
      }
      final String key = ion.getIonType().toString();
      distinct.putIfAbsent(key, member.row());
      involved.add(member.row());
    }

    if (distinct.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.ION_TYPES,
          QualityCheckStatus.UNAVAILABLE, "No ion types annotated", List.of(), involved);
    }

    final String summary =
        distinct.size() + " adduct" + (distinct.size() == 1 ? "" : "s") + ": " + String.join(", ",
            distinct.keySet());
    final List<FeatureListRow> distinctRows = List.copyOf(distinct.values());

    final ColorAssignment coloring = context.colorAssignment();
      return new IonTypesQualityResult(QualityCheckStatus.PASS, summary, distinctRows, involved,
          coloring, context.selectedMemberRow());
  }
}
