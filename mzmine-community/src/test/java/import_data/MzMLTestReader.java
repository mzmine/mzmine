/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package import_data;

import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import javax.xml.stream.XMLInputFactory;
import org.jetbrains.annotations.NotNull;

/**
 * StAX providers available to the local import benchmark.
 */
public enum MzMLTestReader {
  AALTO, WOODSTOX, JDK;

  /**
   * @return a fresh factory for one import
   */
  public @NotNull XMLInputFactory createFactory() {
    return switch (this) {
      case AALTO -> {
        final var factory = new InputFactoryImpl();
        factory.configureForSpeed();
        yield factory;
      }
      case WOODSTOX -> {
        final var factory = new WstxInputFactory();
        factory.configureForSpeed();
        yield factory;
      }
      case JDK -> XMLInputFactory.newDefaultFactory();
    };
  }
}
