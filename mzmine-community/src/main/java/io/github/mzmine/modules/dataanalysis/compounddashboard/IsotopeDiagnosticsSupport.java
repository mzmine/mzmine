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

package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ChargeDiagnostics;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectedComposition;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeSignalAttribution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Developer-only gate and small helpers for the compound dashboard isotope finder diagnostics
 * review tooling. The whole diagnostics UI (envelope overlay, per-peak element labels, per-charge
 * score panel, plausibility colouring, gate band, per-peak dump) is hidden unless the developer
 * opts in via the {@value #ENABLE_PROPERTY} system property, so it never surfaces in user builds.
 */
public final class IsotopeDiagnosticsSupport {

  /**
   * System property that enables the isotope finder diagnostics review UI in the compound
   * dashboard. Off by default. Enable with {@code -Dmzmine.dev.isotopeDiagnostics=true}.
   */
  public static final String ENABLE_PROPERTY = "mzmine.dev.isotopeDiagnostics";

  private IsotopeDiagnosticsSupport() {
  }

  /**
   * @return whether the developer enabled the isotope diagnostics review UI.
   */
  public static boolean isEnabled() {
    return Boolean.getBoolean(ENABLE_PROPERTY);
  }

  /**
   * Find the recomputed diagnostics that match a selected charge-state pattern.
   *
   * @param result  a recomputed detection result (with diagnostics), or null.
   * @param pattern the charge-state pattern currently selected in the mirror, or null.
   * @return the {@link ChargeDiagnostics} whose charge matches the pattern's charge, or null.
   */
  public static @Nullable ChargeDiagnostics matchDiagnostics(@Nullable final DetectionResult result,
      @Nullable final IsotopePattern pattern) {
    if (result == null || result.diagnostics() == null || pattern == null) {
      return null;
    }
    final int charge = pattern.getCharge();
    ChargeDiagnostics fallback = null;
    for (final ChargeDiagnostics d : result.diagnostics()) {
      if (d.charge() == charge) {
        return d;
      }
      if (fallback == null) {
        fallback = d;
      }
    }
    // charge mismatch (e.g. single-scan re-run picked a different winner): fall back to the winner
    // so the reviewer still sees something, rather than a blank overlay.
    return fallback;
  }

  /**
   * Compact human-readable summary of the auto-detected heavy-element composition.
   * <p>
   * The composition is detected for the WINNING charge only (the engine re-scores that charge with
   * it), so it is only reported while an alternate charge is not selected.
   *
   * @param result the recomputed detection result, or null.
   * @param diag   the diagnostics of the charge currently shown, or null.
   * @return the summary, or null when nothing was detected or an alternate charge is shown.
   */
  public static @Nullable String formatComposition(@Nullable final DetectionResult result,
      @Nullable final ChargeDiagnostics diag) {
    if (result == null || diag == null || diag.charge() != result.bestCharge()) {
      return null;
    }
    final DetectedComposition comp = result.detectedComposition();
    if (comp == null || comp.elements().isEmpty()) {
      return null;
    }
    final StringBuilder sb = new StringBuilder("Detected heavy elements: ");
    boolean first = true;
    for (final String el : comp.elements()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(el);
      final Double conf = comp.confidence().get(el);
      if (conf != null) {
        sb.append(String.format(" (%.2f)", conf));
      }
    }
    return sb.toString();
  }

  /**
   * @param result the recomputed detection result, for the detected composition.
   * @param diag   the diagnostics of the charge currently shown, or null.
   * @return a copyable, human-readable dump of the per-signal attribution, the M+1 ratio gate and
   * the detected composition for the selected charge, or a placeholder when no diagnostics exist.
   */
  public static @NotNull String formatDump(@Nullable final DetectionResult result,
      @Nullable final ChargeDiagnostics diag) {
    if (diag == null) {
      return "No diagnostics for the selected charge.";
    }
    final NumberFormats fmt = ConfigService.getGuiFormats();
    final StringBuilder sb = new StringBuilder();
    sb.append("charge z=").append(diag.charge()).append("  baseMz=").append(fmt.mz(diag.baseMz()))
        .append("  monoMz=").append(fmt.mz(diag.monoMz())).append("  placement=")
        .append(diag.placement()).append("  spacing=").append(fmt.mz(diag.spacingDa()))
        .append('\n');
    sb.append(
        String.format("%-12s %-6s %-8s %-14s %s%n", "m/z", "off", "rel%", "assignment", "note"));
    for (final IsotopeSignalAttribution s : diag.signals()) {
      final double predUpper =
          s.offsetFromMono() >= 0 && s.offsetFromMono() < diag.envelopeUpperBound().length
              ? diag.envelopeUpperBound()[s.offsetFromMono()] : 0d;
      final String note =
          predUpper > 0d && s.relIntensity() > predUpper * 1.05 ? "EXCEEDS upperBound" : "";
      sb.append(String.format("%-12s %-6d %-8.1f %-14s %s%n", fmt.mz(s.mz()), s.offsetFromMono(),
          s.relIntensity() * 100d, s.label() + " (" + s.assignment() + ")", note));
    }
    final double[] m1 = diag.m1Bounds();
    if (m1.length == 2) {
      sb.append(String.format("M+1/M gate: [%.4f, %.4f]%n", m1[0], m1[1]));
    }
    final String comp = formatComposition(result, diag);
    if (comp != null) {
      sb.append(comp).append('\n');
    }
    return sb.toString();
  }
}
