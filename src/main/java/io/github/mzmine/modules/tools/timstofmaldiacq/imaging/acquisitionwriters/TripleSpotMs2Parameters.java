package io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class TripleSpotMs2Parameters extends SimpleParameterSet {

  public static final IntegerParameter laserOffsetY = new IntegerParameter("Laser offset Y / µm",
      "Initial offset that is added when moving to a spot.", 0);

  public static final IntegerParameter laserOffsetX = new IntegerParameter("Laser offset X / µm",
      """
          Offset that is added for every acquisition of a precursor list. 
          Recommended = laser spot size
          """, 50);

  public TripleSpotMs2Parameters() {
    super(new Parameter[]{laserOffsetX, laserOffsetY});
  }
}
