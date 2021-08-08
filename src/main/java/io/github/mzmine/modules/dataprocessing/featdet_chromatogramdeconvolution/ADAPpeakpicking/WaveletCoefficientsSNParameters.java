/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
/*
 * author Owen Myers (Oweenm@gmail.com)
 */
package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking;

import java.text.NumberFormat;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.ExitCode;

/**
 *
 * @author owenmyers
 */
public class WaveletCoefficientsSNParameters extends SimpleParameterSet {
  public static final DoubleParameter HALF_WAVELET_WINDOW = new DoubleParameter("Peak width mult.",
      "Singal to noise estimator window size determination.", NumberFormat.getNumberInstance(), 3.0,
      0.0, null);

  public static final BooleanParameter ABS_WAV_COEFFS = new BooleanParameter("abs(wavelet coeffs.)",
      "Do you want to take the absolute value of the wavelet coefficients.", true);

  public WaveletCoefficientsSNParameters() {
    super(new Parameter[] {HALF_WAVELET_WINDOW, ABS_WAV_COEFFS});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    final SNSetUpDialog dialog = new SNSetUpDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
