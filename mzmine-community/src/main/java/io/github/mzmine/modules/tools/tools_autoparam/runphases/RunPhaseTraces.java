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

package io.github.mzmine.modules.tools.tools_autoparam.runphases;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The traces the {@link RunPhases} are derived from, one value per MS1 scan unless noted. For
 * diagnostics and plots.
 *
 * @param rt             retention times of the MS1 scans in minutes
 * @param logTic         log10 TIC
 * @param basePeak       base peak intensity
 * @param suppression    S, median normalized log10 intensity of the background ions
 * @param gradient       G = rising - falling background ions, a solvent composition proxy
 * @param rising         normalized log10 intensity of the rising background ions
 * @param falling        normalized log10 intensity of the falling background ions
 * @param salt           log10 fold change of the sodium formate clusters
 * @param backgroundIons m/z of the background ions
 * @param correlation    correlation of each background ion with the scan index
 * @param saltIon        true for background ions that are sodium formate clusters
 * @param solvent        solvent composition trace in %, null if not available
 * @param pressure       pump pressure trace, null if not available
 */
record RunPhaseTraces(double @NotNull [] rt, double @NotNull [] logTic, double @NotNull [] basePeak,
                      double @NotNull [] suppression, double @NotNull [] gradient,
                      double @NotNull [] rising, double @NotNull [] falling,
                      double @NotNull [] salt, double @NotNull [] backgroundIons,
                      double @NotNull [] correlation, boolean @NotNull [] saltIon,
                      @Nullable PumpTrace solvent, @Nullable PumpTrace pressure) {

}
