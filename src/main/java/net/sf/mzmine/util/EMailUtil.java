/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util;

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
public class EMailUtil {

  /**
   * Method to send simple HTML email
   */
  public static void sendEmail(Session session, String toEmail, String subject, String body) {
    Logger logger = Logger.getLogger("Mail Error");
    try {
      MimeMessage msg = new MimeMessage(session);
      // set message headers
      msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
      msg.addHeader("format", "flowed");
      msg.addHeader("Content-Transfer-Encoding", "8bit");

      msg.setFrom(new InternetAddress(toEmail, "MZmine"));

      msg.setSubject(subject, "UTF-8");

      msg.setText("MZmine 2 has detected an error :\n" + body, "UTF-8");

      msg.setSentDate(new Date());

      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));

      Transport.send(msg);

      logger.info("Successfully sended error mail: " + subject + "\n" + body);
    } catch (Exception e) {
      logger.info("Failed sending error mail:" + subject + "\n" + body);
      e.printStackTrace();
    }
  }

}
