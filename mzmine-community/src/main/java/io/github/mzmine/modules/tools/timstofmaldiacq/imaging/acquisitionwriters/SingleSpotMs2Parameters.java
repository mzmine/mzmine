/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class SingleSpotMs2Parameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> laserOffsetX = new OptionalParameter<>(
      new IntegerParameter("Laser offset X / µm",
          "Laser offset after moving to the spot where the MS1 spectrum was acquired.", 0), false);
  public static final OptionalParameter<IntegerParameter> laserOffsetY = new OptionalParameter<>(
      new IntegerParameter("Laser offset Y / µm",
          "Laser offset after moving to the spot where the MS1 spectrum was acquired.", 0), false);

  public SingleSpotMs2Parameters() {
    super(new Parameter[]{laserOffsetX, laserOffsetY});
  }

  public SingleSpotMs2Parameters(boolean xEnabled, int xOffset, boolean yEnabled, int yOffset) {
    this();
    this.setParameter(laserOffsetX, xEnabled);
    this.getParameter(laserOffsetX).getEmbeddedParameter().setValue(xOffset);
    this.setParameter(laserOffsetY, yEnabled);
    this.getParameter(laserOffsetY).getEmbeddedParameter().setValue(xOffset);
  }
}
