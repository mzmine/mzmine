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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import java.util.Comparator;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Potential issues of a {@link ChromatogramGroup}, ordered by severity. Expected differences
 * between the builders come last.
 */
public enum ChromatogramIssue {
  ONLY_A(ComparisonSide.A, "only A", "Only list A has a chromatogram at this m/z"), //
  ONLY_B(ComparisonSide.B, "only B", "Only list B has a chromatogram at this m/z"), //
  MISSING_A(ComparisonSide.A, "missing in A",
      "A has no data point in scans where B has a signal above the threshold"), //
  MISSING_B(ComparisonSide.B, "missing in B",
      "B has no data point in scans where A has a signal above the threshold"), //
  SPLIT_A(ComparisonSide.A, "split in A", splitDescription("A")), //
  SPLIT_B(ComparisonSide.B, "split in B", splitDescription("B")), //
  HOLES_A(ComparisonSide.A, "holes in A", "A leaves out data points of the mass lists in short "
      + "gaps between two signals above the threshold (up to "
      + ChromatogramComparison.MAX_HOLE_SCANS + " scans)"), //
  HOLES_B(ComparisonSide.B, "holes in B", "B leaves out data points of the mass lists in short "
      + "gaps between two signals above the threshold (up to "
      + ChromatogramComparison.MAX_HOLE_SCANS + " scans)"), //
  MULTIPLE_A(ComparisonSide.A, "multiple in A", """
      A has more than one chromatogram in this group, separate peaks or co-eluting ions that B \
      has in one chromatogram"""), //
  MULTIPLE_B(ComparisonSide.B, "multiple in B", """
      B has more than one chromatogram in this group, separate peaks or co-eluting ions that A \
      has in one chromatogram"""), //
  TAKEN_A(ComparisonSide.A, "taken in A", takenDescription("A", "B")), //
  TAKEN_B(ComparisonSide.B, "taken in B", takenDescription("B", "A")), //
  DIFFERENT_DATA_POINTS(null, "different data points",
      "A and B have a different data point in the same scan, one above the threshold"), //
  ONLY_A_LOW_SEGMENT(ComparisonSide.A, "only A, low segment", lowSegmentDescription("A", "B")), //
  ONLY_B_LOW_SEGMENT(ComparisonSide.B, "only B, low segment", lowSegmentDescription("B", "A"));

  /**
   * Sorts by the most severe issue first, groups without issues last.
   */
  public static final Comparator<Set<ChromatogramIssue>> SEVERITY_ORDER = Comparator.<Set<ChromatogramIssue>>comparingInt(
      ChromatogramIssue::mostSevereOrdinal).thenComparing(Set::size, Comparator.reverseOrder());

  private final @Nullable ComparisonSide side;
  private final @NotNull String label;
  private final @NotNull String description;

  ChromatogramIssue(@Nullable ComparisonSide side, @NotNull String label,
      @NotNull String description) {
    this.side = side;
    this.label = label;
    this.description = description;
  }

  private static @NotNull String splitDescription(@NotNull String side) {
    return """
        %s has chromatograms in this group that never share a scan, and one continues the \
        intensity next to the apex of the other: one ion split into several chromatograms, e.g., \
        by an m/z shift of the intense apex""".formatted(side);
  }

  private static @NotNull String takenDescription(@NotNull String side, @NotNull String other) {
    return """
        %1$s has no data point in scans where %2$s has a signal above the threshold, but another \
        chromatogram of %1$s holds all of them and the most intense in at least the min \
        consecutive scans of the builder (see Taken by), so the resolver can resolve the peak \
        there. E.g., a separate peak next to an intense ion that the fast chromatogram builder \
        joins into the channel of the ion""".formatted(side, other);
  }

  private static @NotNull String lowSegmentDescription(@NotNull String side,
      @NotNull String other) {
    return """
        Only list %1$s has a chromatogram at this m/z, and it fails the filter of the chromatogram \
        builder of %2$s: no segment of consecutive scans above the min group intensity reaches the \
        min height. Expected, the fast chromatogram builder needs the min height within the \
        segment, ADAP anywhere in the chromatogram""".formatted(side, other);
  }

  private static int mostSevereOrdinal(@NotNull Set<ChromatogramIssue> issues) {
    return issues.stream().mapToInt(Enum::ordinal).min().orElse(Integer.MAX_VALUE);
  }

  public static @NotNull ChromatogramIssue only(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? ONLY_A : ONLY_B;
  }

  public static @NotNull ChromatogramIssue onlyLowSegment(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? ONLY_A_LOW_SEGMENT : ONLY_B_LOW_SEGMENT;
  }

  public static @NotNull ChromatogramIssue split(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? SPLIT_A : SPLIT_B;
  }

  public static @NotNull ChromatogramIssue missing(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? MISSING_A : MISSING_B;
  }

  public static @NotNull ChromatogramIssue taken(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? TAKEN_A : TAKEN_B;
  }

  public static @NotNull ChromatogramIssue holes(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? HOLES_A : HOLES_B;
  }

  public static @NotNull ChromatogramIssue multiple(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? MULTIPLE_A : MULTIPLE_B;
  }

  /**
   * @return the side with the issue, null if the issue concerns both sides
   */
  public @Nullable ComparisonSide getSide() {
    return side;
  }

  /**
   * @return true for differences that follow from the different filters of the builders
   */
  public boolean isExpected() {
    return this == ONLY_A_LOW_SEGMENT || this == ONLY_B_LOW_SEGMENT;
  }

  public @NotNull String getLabel() {
    return label;
  }

  public @NotNull String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return label;
  }
}
