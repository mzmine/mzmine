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

package io.github.mzmine.modules.visualization.dash_lipidqc.retention;

import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;

/**
 * Synchronises two JFreeChart {@link NumberAxis} instances so that when a user zooms one axis the
 * selected data point stays visible at the same relative position on both axes.
 */
final class SelectedPointAxisSynchronizer {

  private final @NotNull NumberAxis primaryAxis;
  private final @NotNull NumberAxis secondaryAxis;
  private final double primaryValue;
  private final double secondaryValue;
  private final double primaryMinBound;
  private final double primaryMaxBound;
  private final double secondaryMinBound;
  private final double secondaryMaxBound;
  private final @NotNull AxisChangeListener primaryListener;
  private final @NotNull AxisChangeListener secondaryListener;
  private boolean updating;

  SelectedPointAxisSynchronizer(final @NotNull NumberAxis primaryAxis,
      final @NotNull NumberAxis secondaryAxis, final double primaryValue,
      final double secondaryValue, final double primaryMinBound, final double primaryMaxBound,
      final double secondaryMinBound, final double secondaryMaxBound) {
    this.primaryAxis = primaryAxis;
    this.secondaryAxis = secondaryAxis;
    this.primaryValue = primaryValue;
    this.secondaryValue = secondaryValue;
    this.primaryMinBound = primaryMinBound;
    this.primaryMaxBound = primaryMaxBound;
    this.secondaryMinBound = secondaryMinBound;
    this.secondaryMaxBound = secondaryMaxBound;
    primaryListener = this::onPrimaryAxisChanged;
    secondaryListener = this::onSecondaryAxisChanged;
  }

  void install() {
    primaryAxis.addChangeListener(primaryListener);
    secondaryAxis.addChangeListener(secondaryListener);
  }

  void syncSecondaryToPrimary() {
    syncAxis(primaryAxis, secondaryAxis, primaryValue, secondaryValue, secondaryMinBound,
        secondaryMaxBound);
  }

  private void onPrimaryAxisChanged(final @NotNull AxisChangeEvent event) {
    syncSecondaryToPrimary();
  }

  private void onSecondaryAxisChanged(final @NotNull AxisChangeEvent event) {
    syncPrimaryToSecondary();
  }

  private void syncPrimaryToSecondary() {
    syncAxis(secondaryAxis, primaryAxis, secondaryValue, primaryValue, primaryMinBound,
        primaryMaxBound);
  }

  private void syncAxis(final @NotNull NumberAxis sourceAxis,
      final @NotNull NumberAxis targetAxis, final double sourceValue,
      final double targetValue, final double targetMinBound, final double targetMaxBound) {
    if (updating || !Double.isFinite(sourceValue) || !Double.isFinite(targetValue)) {
      return;
    }
    final org.jfree.data.Range sourceRange = sourceAxis.getRange();
    final org.jfree.data.Range targetRange = targetAxis.getRange();
    final double sourceSpan = sourceRange.getLength();
    final double targetSpan = targetRange.getLength();
    if (!Double.isFinite(sourceSpan) || !Double.isFinite(targetSpan) || sourceSpan <= 0d
        || targetSpan <= 0d) {
      return;
    }

    final double normalizedPosition = (sourceValue - sourceRange.getLowerBound()) / sourceSpan;
    if (!Double.isFinite(normalizedPosition)) {
      return;
    }
    final double clampedPosition = EquivalentCarbonNumberPane.clampToUnit(normalizedPosition);
    double adaptedSpan = targetSpan;
    if (Double.isFinite(targetMinBound) && Double.isFinite(targetMaxBound)
        && targetMaxBound >= targetMinBound) {
      if (clampedPosition > 0d) {
        adaptedSpan = Math.max(adaptedSpan, (targetValue - targetMinBound) / clampedPosition);
      }
      if (clampedPosition < 1d) {
        adaptedSpan = Math.max(adaptedSpan,
            (targetMaxBound - targetValue) / (1d - clampedPosition));
      }
    }
    if (!Double.isFinite(adaptedSpan) || adaptedSpan <= 0d) {
      adaptedSpan = targetSpan;
    }
    final double targetLower = targetValue - clampedPosition * adaptedSpan;
    final double targetUpper = targetLower + adaptedSpan;
    updating = true;
    try {
      targetAxis.setRange(targetLower, targetUpper);
    } finally {
      updating = false;
    }
  }
}
