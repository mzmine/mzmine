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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Deep copy of any serializable object
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class BinaryWriterReader {
  private FileOutputStream FOS = null;
  private ObjectOutputStream OOS = null;
  private FileInputStream FIS = null;
  private ObjectInputStream OIS = null;

  public BinaryWriterReader() {}

  public void save2file(Object obj, File file) {
    if (OOS == null || FOS == null) {
      open_out(file);
    }

    try {
      OOS.writeObject(obj);
    } catch (IOException ioe) {
      System.err.println("Error: Could not serialize object.");
      ioe.printStackTrace(System.err);
    }
  }

  public Object readFromFile(File file) {
    if (OIS == null || FIS == null) {
      open_in(file);
    }

    try {
      Object obj = (Object) OIS.readObject();
      return obj;
    } catch (IOException ioe) {
      System.err.println("Error: Could not deserialize object.");
      ioe.printStackTrace(System.err);
    } catch (ClassNotFoundException cnfe) {
      System.err.println("Error: Could not find class!");
      cnfe.printStackTrace(System.err);
    }
    return null;
  }

  private void open_out(File file) {
    if (OOS != null || FOS != null) {
      closeOut();
    }

    try {
      FOS = new FileOutputStream(file);
      OOS = new ObjectOutputStream(FOS);
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
      ioe.printStackTrace(System.out);
    }
  }

  private void open_in(File file) {
    if (FIS != null || OIS != null) {
      closeIn();
    }

    try {
      FIS = new FileInputStream(file);
      OIS = new ObjectInputStream(FIS);
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
      ioe.printStackTrace(System.out);
    }
  }

  public void closeOut() {
    if (OOS != null && FOS != null) {
      try {
        OOS.close();
        OOS = null;
        FOS.close();
        FOS = null;
      } catch (IOException ioe) {
        System.err.println(ioe.getMessage());
        ioe.printStackTrace(System.out);
      }
    }

  }

  public void closeIn() {
    if (OIS != null && FIS != null) {
      try {
        OIS.close();
        OIS = null;
        FIS.close();
        FIS = null;
      } catch (IOException ioe) {
        System.err.println(ioe.getMessage());
        ioe.printStackTrace(System.out);
      }
    }
  }

  /**
   * Deep copy of a serializable object.
   * 
   * @param o
   * @return
   * @throws Exception
   */
  public static <T> T deepCopy(T o) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(o);

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

    return (T) new ObjectInputStream(bais).readObject();
  }
}
