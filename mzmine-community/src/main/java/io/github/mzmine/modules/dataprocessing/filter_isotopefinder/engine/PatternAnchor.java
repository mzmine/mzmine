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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

import org.jetbrains.annotations.NotNull;

/**
 * How one detected pattern is anchored to its predicted envelope, so a consumer of the pattern can
 * map an m/z back to a predicted isotope offset. Without this, an m/z can only be mapped to an
 * offset relative to some observed signal, which says nothing about the predicted intensity there.
 * <p>
 * A signal at {@code mz} sits at predicted offset
 * {@code round((mz - baseMz) / env.spacingDa()) + placement}.
 *
 * @param env       the predicted envelope the charge hypothesis was scored against.
 * @param baseMz    m/z of the observed base peak (the most intense signal), i.e. observed offset 0.
 * @param placement the predicted offset that the sliding envelope fit aligned to observed offset 0.
 */
public record PatternAnchor(@NotNull IsotopeEnvelope env, double baseMz, int placement) {

  /**
   * @param mz an observed m/z.
   * @return the predicted offset (relative to the monoisotopic) this m/z falls on.
   */
  public int predictedOffsetOf(final double mz) {
    return (int) Math.round((mz - baseMz) / env.spacingDa()) + placement;
  }
}
