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

package io.github.mzmine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Internet related utilities
 */
public class InetUtils {

  private static final Logger logger = Logger.getLogger(InetUtils.class.getName());

  /**
   * Opens a connection to the given URL (typically HTTP) and retrieves the data from server. Data
   * is assumed to be in UTF-8 encoding.
   */
  public static String retrieveData(URL url) throws IOException {

    logger.finest("Retrieving data from URL " + url);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty("User-agent", "MZmine");

    // We need to deal with redirects from HTTP to HTTPS manually, this
    // happens for MassBank Europe
    // Based on
    // https://stackoverflow.com/questions/1884230/urlconnection-doesnt-follow-redirect
    switch (connection.getResponseCode()) {
      case HttpURLConnection.HTTP_MOVED_PERM:
      case HttpURLConnection.HTTP_MOVED_TEMP:
        String location = connection.getHeaderField("Location");
        location = URLDecoder.decode(location, StandardCharsets.UTF_8);
        URL next = new URL(url, location); // Deal with relative URLs
        connection.disconnect();
        connection = (HttpURLConnection) next.openConnection();
        connection.setRequestProperty("User-agent", "MZmine");
    }

    InputStream is = connection.getInputStream();

    if (is == null) {
      throw new IOException("Could not establish a connection to " + url);
    }

    StringBuffer buffer = new StringBuffer();

    try {
      InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

      char[] cb = new char[1024];

      int amtRead = reader.read(cb);
      while (amtRead > 0) {
        buffer.append(cb, 0, amtRead);
        amtRead = reader.read(cb);
      }
      reader.close();
    } catch (UnsupportedEncodingException e) {
      // This should never happen, because UTF-8 is supported
      e.printStackTrace();
    }

    is.close();

    logger.finest("Retrieved " + buffer.length() + " characters from " + url);

    return buffer.toString();

  }

}
