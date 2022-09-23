package io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class SingleSpotMs2Parameters extends SimpleParameterSet {

  public static final IntegerParameter laserOffsetX = new IntegerParameter("Laser offset X / µm",
      "Laser offset after moving to the spot where the MS1 spectrum was acquired.", 0);
  public static final IntegerParameter laserOffsetY = new IntegerParameter("Laser offset Y / µm",
      "Laser offset after moving to the spot where the MS1 spectrum was acquired.", 0);

  public SingleSpotMs2Parameters() {
    super(new Parameter[]{laserOffsetX, laserOffsetY});
  }
}
