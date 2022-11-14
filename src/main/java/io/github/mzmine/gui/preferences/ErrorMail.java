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

package io.github.mzmine.gui.preferences;

import java.io.IOException;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import io.github.mzmine.util.EMailUtil;

/**
 * Class to send an error messsage only smtp support
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ErrorMail {

  public void sendErrorEmail(String emailTo, String emailFrom, String smtpServer, String subject,
      String msg, String password, Integer port) throws IOException {

    Properties props = new Properties();
    props.put("mail.smtp.host", smtpServer); // SMTP Host
    props.put("mail.smtp.socketFactory.port", port); // SSL Port
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // SSL Factory
                                                                                  // Class
    props.put("mail.smtp.auth", "true"); // Enabling SMTP Authentication
    props.put("mail.smtp.port", port); // SMTP Port, gmail 465

    Authenticator auth = new Authenticator() {

      // override the getPasswordAuthentication method
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(emailFrom, password);
      }
    };
    Session session = Session.getDefaultInstance(props, auth);
    Thread eMailThread = new Thread(new EMailUtil(session, emailTo, subject, msg));
    eMailThread.start();
  }
}
