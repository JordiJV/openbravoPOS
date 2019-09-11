//jjuanmarti
package com.openbravo.pos.util;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class SendEmail {
    

    
    /** Creates a new instance of SendEmail */
    public SendEmail() {
    }
    
    public void send(String filePath, String to, String subject, String filename, StringBuilder content) {
    	
        // Sender's email ID needs to be mentioned
        String from = "info@dulcinenca.com";

        final String username = "info@dulcinenca.com";//change accordingly
        final String password = "P}gLFP-ZQa=S";//change accordingly

        // Assuming you are sending email through relay.jangosmtp.net
        String host = "mail.dulcinenca.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        //props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "26");

        // Get the Session object.
        Session session = Session.getInstance(props,
           new javax.mail.Authenticator() {
              protected PasswordAuthentication getPasswordAuthentication() {
                 return new PasswordAuthentication(username, password);
              }
           });

        try {
           // Create a default MimeMessage object.
           Message message = new MimeMessage(session);

           // Set From: header field of the header.
           message.setFrom(new InternetAddress(from));

           // Set To: header field of the header.
           message.setRecipients(Message.RecipientType.TO,
              InternetAddress.parse(to));

           // Set Subject: header field
           message.setSubject(subject);

           // Create the message part
           BodyPart messageBodyPart = new MimeBodyPart();

           // Now set the actual message
           if (content!=null) {
        	   messageBodyPart.setText(content.toString());
           } else {
        	   messageBodyPart.setText("");
           }

           // Create a multipar message
           Multipart multipart = new MimeMultipart();

           // Set text message part
           multipart.addBodyPart(messageBodyPart);

           // Part two is attachment
           if (filePath!=null) {
	           messageBodyPart = new MimeBodyPart();
	           DataSource source = new FileDataSource(filePath);
	           messageBodyPart.setDataHandler(new DataHandler(source));
	           messageBodyPart.setFileName(filename);
	           multipart.addBodyPart(messageBodyPart);
           }

           // Send the complete message parts
           message.setContent(multipart);

           // Send message
           Transport.send(message);

           //System.out.println("Sent message successfully....");
    
        } catch (MessagingException e) {
           throw new RuntimeException(e);
        }
    }
    
    
}
