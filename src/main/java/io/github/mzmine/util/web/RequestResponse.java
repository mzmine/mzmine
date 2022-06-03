/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.web;

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

  public void openURL() {
    WebUtils.openURL(url);
  }

  public boolean isSuccess() {
    return statusCode / 200 == 1;
  }
}
