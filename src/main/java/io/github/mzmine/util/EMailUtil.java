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

import java.util.Date;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Simple e-Mail util for sending error messages
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class EMailUtil implements Runnable {

  private Session session;
  private String toEmail;
  private String subject;
  private String body;

  public EMailUtil(Session session, String toEmail, String subject, String body) {
    this.session = session;
    this.toEmail = toEmail;
    this.subject = subject;
    this.body = body;
  }

  /**
   * Method to send simple HTML email
   */
  private void sendEmail() {

    Logger logger = Logger.getLogger("Mail Error");
    try {
      MimeMessage msg = new MimeMessage(session);
      // set message headers
      msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
      msg.addHeader("format", "flowed");
      msg.addHeader("Content-Transfer-Encoding", "8bit");

      msg.setFrom(new InternetAddress(toEmail, "MZmine"));

      msg.setSubject(subject, "UTF-8");

      msg.setText("MZmine has detected an error :\n" + body, "UTF-8");

      msg.setSentDate(new Date());

      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));

      Transport.send(msg);

      logger.info("Successfully sended error mail: " + subject + "\n" + body);
    } catch (Exception e) {
      logger.info("Failed sending error mail:" + subject + "\n" + body);
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    sendEmail();
  }

}
