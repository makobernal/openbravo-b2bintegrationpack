package org.openbravo.b2bintegration.base.emailengine;

import java.util.Date;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.b2bintegration.B2bEmailInstance;
import org.openbravo.b2bintegration.B2bEmailInstanceParam;
import org.openbravo.b2bintegration.B2bEmailInstanceRecipient;
import org.openbravo.b2bintegration.B2bEmailTemplate;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;

public class EmailInstance {

  private B2bEmailInstance newEmailInstance;

  public EmailInstance() throws ServletException {
    newEmailInstance = OBProvider.getInstance().get(B2bEmailInstance.class);
  }

  public void setTemplate(String templateValue, String lang) {

    // Retrieve Language
    OBCriteria<Language> obc = OBDal.getInstance().createCriteria(Language.class);
    obc.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));

    // Retrieve Template
    OBCriteria<B2bEmailTemplate> obc2 = OBDal.getInstance().createCriteria(B2bEmailTemplate.class);
    obc2.add(Restrictions.eq(B2bEmailTemplate.PROPERTY_SEARCHKEY, templateValue));
    obc2.add(Restrictions.eq(B2bEmailTemplate.PROPERTY_LANGUAGEID, obc.list().get(0)));

    newEmailInstance.setB2BEmailtemplate(obc2.list().get(0));
    newEmailInstance.setUserContact(OBContext.getOBContext().getUser());
    newEmailInstance.setDatecreated(new Date());
    newEmailInstance.setSentStatus("UNSENT");

    OBDal.getInstance().save(newEmailInstance);
    OBDal.getInstance().flush();

  }

  public void addRecipient(String type, String recipientEmail) {
    B2bEmailInstanceRecipient newRecipient = OBProvider.getInstance().get(
        B2bEmailInstanceRecipient.class);
    newRecipient.setRecipienttype(type);
    newRecipient.setEmail(recipientEmail);
    newRecipient.setB2BEmailinstance(newEmailInstance);
    OBDal.getInstance().save(newRecipient);
    OBDal.getInstance().flush();

    newEmailInstance.getB2BEmailinstanceRecipientList().add(newRecipient);
  }

  private void addParameter(String paramName, String paramValue, String paramType) {

    B2bEmailInstanceParam newParameter = OBProvider.getInstance().get(B2bEmailInstanceParam.class);
    newParameter.setParamtype(paramType);
    newParameter.setParamname(paramName);
    newParameter.setParamvalue(paramValue);
    newParameter.setB2BEmailinstance(newEmailInstance);
    OBDal.getInstance().save(newParameter);
    OBDal.getInstance().flush();

    newEmailInstance.getB2BEmailinstanceParamList().add(newParameter);

  }

  public void addBodyParameter(String paramName, String paramValue) {
    addParameter(paramName, paramValue, "STD");
  }

  public void addSubjectParameter(String paramName, String paramValue) {
    addParameter(paramName, paramValue, "SUB");
  }

  public B2bEmailInstance saveAndClose() {

    newEmailInstance.setSentStatus("READY");

    OBDal.getInstance().flush();
    return newEmailInstance;
  }

  public void addToRecipient(String email) {
    addRecipient("TO", email);
  }

  public void addCcRecipient(String email) {
    addRecipient("CC", email);
  }

  public void addBccRecipient(String email) {
    addRecipient("BCC", email);
  }

}