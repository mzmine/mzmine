/*
 * Copyright 2006-2015 The du-lab Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
 /*
 * author Owen Myers (Oweenm@gmail.com)
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking;

import javax.annotation.Nonnull;
import net.sf.mzmine.parameters.ParameterSet;

public class WaveletCoefficientsSNEstimator  implements SNEstimatorChoice{
    @Override
    public @Nonnull
    String getName() {
        return "Wavelet Coeff. SN";
    }
    
    public String getSNCode(){
        return "Wavelet Coefficient Estimator";
    }
    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return WaveletCoefficientsSNParameters.class;
    }
    
        @Override
    public boolean getRequiresR() {
        return false;
    }

    @Override
    public String[] getRequiredRPackages() {
        return null;
    }

    @Override
    public String[] getRequiredRPackagesVersions() {
        return null;
    }
}
