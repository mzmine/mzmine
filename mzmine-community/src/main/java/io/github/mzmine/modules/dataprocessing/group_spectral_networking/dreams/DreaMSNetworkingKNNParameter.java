/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;

public class DreaMSNetworkingKNNParameter extends SimpleParameterSet {

    public static final IntegerParameter numNeighbors = new IntegerParameter("Num. neighbors",
                                 "The number of nearest neighbors to display for each node in the network. If left " +
                                         "empty, all edges will be displayed. Note, edges with similarity higher than " +
                                         "the main ‘Min similarity’ parameter will always be retained, regardless of the " +
                                         "setting of this parameter.",
                                 3, true);

    public static final PercentParameter minScoreNeighbors = new PercentParameter("Min neighbor similarity",
            "Edges with similarity below this threshold will be filtered out. ", 0.6, 0.0, 1.0);

    public DreaMSNetworkingKNNParameter() {
        super(numNeighbors, minScoreNeighbors);
    }

}
