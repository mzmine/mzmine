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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters;

import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;

public enum MaldiMs2AcquisitionWriters implements ModuleOptionsEnum<MaldiMs2AcquisitionWriter> {
  SINGLE_SPOT, TRIPLE_SPOT;

  public static MaldiMs2AcquisitionWriter createDefault() {
    return new SingleSpotMs2Writer();
  }

  public static MaldiMs2AcquisitionWriter createOption(
      final ValueWithParameters<MaldiMs2AcquisitionWriters> param) {
    return switch (param.value()) {
      case SINGLE_SPOT -> new SingleSpotMs2Writer(param.parameters());
      case TRIPLE_SPOT -> new TripleSpotMs2Writer(param.parameters());
    };
  }

  @Override
  public String toString() {
    return getStableId();
  }

  @Override
  public Class<? extends MaldiMs2AcquisitionWriter> getModuleClass() {
    return switch (this) {
      case SINGLE_SPOT -> SingleSpotMs2Writer.class;
      case TRIPLE_SPOT -> TripleSpotMs2Writer.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case SINGLE_SPOT -> "Single spot";
      case TRIPLE_SPOT -> "Triple spot";
    };
  }
}
