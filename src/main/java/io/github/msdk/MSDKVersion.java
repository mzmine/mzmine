/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk;

import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nonnull;

/**
 * MSDK version
 */
public class MSDKVersion {

  /**
   * <p>
   * getMSDKVersion.
   * </p>
   *
   * @return a {@link String} object.
   */
  public static @Nonnull String getMSDKVersion() {
    try {
      ClassLoader myClassLoader = MSDKVersion.class.getClassLoader();
      InputStream inStream = myClassLoader.getResourceAsStream("version.properties");
      if (inStream == null)
        throw new MSDKRuntimeException("Cannot read MSDK version");
      Properties properties = new Properties();
      properties.load(inStream);
      String value = properties.getProperty("version");
      if (value == null)
        throw new MSDKRuntimeException("Cannot read MSDK version");
      else
        return value;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

}
