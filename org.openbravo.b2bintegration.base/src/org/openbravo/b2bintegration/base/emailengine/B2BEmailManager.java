package org.openbravo.b2bintegration.base.emailengine;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;
import org.openbravo.b2bintegration.B2bEmailInstance;
import org.openbravo.b2bintegration.B2bEmailInstanceParam;
import org.openbravo.b2bintegration.B2bEmailInstanceRecipient;
import org.openbravo.b2bintegration.B2bGlobalParameters;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.utils.FormatUtilities;

public class B2BEmailManager {
  private static B2bGlobalParameters b2bglobalparams;

  public static void send(B2bEmailInstance b2bEmailInstance) throws Exception {
    send(b2bEmailInstance, false);
  }

  public static void send(B2bEmailInstance b2bEmailInstance, Boolean isdebug) throws Exception {

    // Get Global params
    b2bglobalparams = getB2BGlobalParams();

    Properties props = new Properties();
    props.put("mail.debug", isdebug ? "true" : "false");
    props.put("mail.mime.charset", "UTF-8");
    props.put("mail.smtp.auth", (b2bglobalparams.isSMTPAuthentification() ? "true" : "false"));
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.mail.sender", b2bglobalparams.getSmtpServerSenderAddress());
    props.put("mail.host", b2bglobalparams.getSmtpServer());
    props.put("mail.smtp.starttls.enable", (b2bglobalparams.isUsetls() ? "true" : "false"));

    ClientAuthenticator authenticator = null;
    if (b2bglobalparams.getSmtpServerAccount() != null) {
      authenticator = new ClientAuthenticator(b2bglobalparams.getSmtpServerAccount(),
          FormatUtilities.encryptDecrypt(b2bglobalparams.getSmtpServerPassword(), false));
    }

    Session session = Session.getInstance(props, authenticator);
    Message message = new MimeMessage(session);

    message.setFrom(new InternetAddress(b2bglobalparams.getSmtpServerSenderAddress()));

    // Set recipients

    setRecipients(message, b2bEmailInstance.getB2BEmailinstanceRecipientList());

    // Set Subject

    message.setSubject(getSubject(b2bEmailInstance));

    // Set html content
    // getHtmlBody(b2bEmailInstance));

    final Multipart multipart = new MimeMultipart();
    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(getHtmlBody(b2bEmailInstance), "text/html");
    multipart.addBodyPart(messageBodyPart);

    // message.setHeader("Content-Type","text/plain; charset=\"iso-8859-1\"");
    message.setContent("This email should have been HTML", "text/plain");
    message.setContent(multipart);

    Transport.send(message);

    b2bEmailInstance.setSentStatus("SENT");
    b2bEmailInstance.setDatesent(new Date());

  }

  private static String getHtmlBody(B2bEmailInstance b2bEmailInstance) throws Exception {

    String templatefilepath = b2bEmailInstance.getB2BEmailtemplate().getTemplatefilepath();

    templatefilepath = templatefilepath.replace("@base_web@", "web");

    String base_web = new ConfigParameters(OBConfigFileProvider.getInstance().getServletContext()).prefix;

    File templatefile = new File(base_web + templatefilepath);

    String bodyTemplate = FileUtils.readFileToString(templatefile);

    for (B2bEmailInstanceParam subjectParam : b2bEmailInstance.getB2BEmailinstanceParamList()) {
      if (subjectParam.getParamtype().equals("STD")) {
        bodyTemplate = bodyTemplate.replace(subjectParam.getParamname(), subjectParam
            .getParamvalue());
      }
    }
    return bodyTemplate;
  }

  private static String getSubject(B2bEmailInstance b2bEmailInstance) {

    String subjectTemplate = b2bEmailInstance.getB2BEmailtemplate().getEmailsubject();

    for (B2bEmailInstanceParam subjectParam : b2bEmailInstance.getB2BEmailinstanceParamList()) {
      if (subjectParam.getParamtype().equals("SUB")) {
        subjectTemplate = subjectTemplate.replace(subjectParam.getParamname(), subjectParam
            .getParamvalue());
      }
    }
    return subjectTemplate;
  }

  private static void setRecipients(Message message, List<B2bEmailInstanceRecipient> recipientlist)
      throws Exception {

    for (B2bEmailInstanceRecipient recipient : recipientlist) {

      Message.RecipientType recipienttype = findRecipientType(recipient.getRecipienttype());

      message.addRecipient(recipienttype, new InternetAddress(recipient.getEmail()));
    }

  }

  private static RecipientType findRecipientType(String recipienttype) {

    if (recipienttype.equals("TO")) {
      return Message.RecipientType.TO;
    } else if (recipienttype.equals("CC")) {
      return Message.RecipientType.CC;
    } else if (recipienttype.equals("BCC")) {
      return Message.RecipientType.BCC;
    } else {
      return null;
    }
  }

  private static B2bGlobalParameters getB2BGlobalParams() throws Exception {
    OBCriteria<B2bGlobalParameters> obc = OBDal.getInstance().createCriteria(
        B2bGlobalParameters.class);

    if (obc.list().isEmpty()) {
      throw new Exception("@b2b_noglobalparameters@");
    }
    return obc.list().get(0);
  }
}
