package io.github.mzmine.parameters.impl;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import java.util.Map;

/**
 * Parameter set with a version above 1 that records the version it was loaded with.
 */
public class VersionedTestParameters extends SimpleParameterSet {

  public static final int VERSION = 3;
  public static final DoubleParameter value = new DoubleParameter("Value", "");

  private int loadedVersion = -1;

  public VersionedTestParameters() {
    super(value);
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams,
      final int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);
    this.loadedVersion = loadedVersion;
  }

  /**
   * @return the version passed to {@link #handleLoadedParameters}, -1 if not loaded
   */
  public int getLoadedVersion() {
    return loadedVersion;
  }
}
