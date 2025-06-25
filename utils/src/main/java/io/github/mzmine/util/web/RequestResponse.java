/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.web;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple representation of a response from web api. Extracts the URL from the json or text
 * response
 *
 * @param response   response text
 * @param url        extracted URL or empty string
 * @param statusCode the status code from the request
 * @author <a href="https://github.com/robinschmid">Robin Schmid</a>
 */
public record RequestResponse(String response, String url, int statusCode) {

  private static final Logger logger = Logger.getLogger(RequestResponse.class.getName());
  public static RequestResponse NONE = new RequestResponse("", "", -1);

  public RequestResponse(final HttpResponse<String> response) {
    this(response.body(), response.uri().toString(), response.statusCode());
  }

  public void openURL() {
    if (url != null && Desktop.isDesktopSupported() && !url.isBlank()) {
      try {
        Desktop.getDesktop().browse(new URI(url));
      } catch (IOException | URISyntaxException e) {
        logger.log(Level.WARNING, "Cannot open browser for URL: " + url, e);
      }
    }
  }

  public boolean isSuccess() {
    return statusCode / 200 == 1;
  }
}
