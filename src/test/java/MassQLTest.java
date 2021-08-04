/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

import io.github.mzmine.util.webapi.MassQLUtils;
import io.github.mzmine.util.webapi.WebAPIUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MassQLTest {

  private static final Logger logger = Logger.getLogger(MassQLTest.class.getName());
  private final static String EXAMPLE_REQUEST = "QUERY scaninfo(MS2DATA) WHERE MS2PROD=660.2:TOLERANCEMZ=0.1";


  @Test
  void testRequestToJson() throws URISyntaxException, IOException, InterruptedException {
    JSONObject json = MassQLUtils.parseQueryGetJson(EXAMPLE_REQUEST);
    Assertions.assertNotNull(json);
    logger.info(json.toString());
  }

  @Test
  void testRequest() throws URISyntaxException, IOException, InterruptedException {
    HttpResponse<String> response = MassQLUtils.parseQuery(EXAMPLE_REQUEST);
    Assertions.assertNotNull(response);
    Assertions.assertTrue(WebAPIUtils.isSuccessful(response), "Status code signals no success");
    logger.info("Status code: " + response.statusCode());
    logger.info(response.body());
  }
}
