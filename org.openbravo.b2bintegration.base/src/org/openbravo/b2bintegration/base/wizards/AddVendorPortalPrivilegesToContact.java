package org.openbravo.b2bintegration.base.wizards;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.b2bintegration.B2bEmailInstance;
import org.openbravo.b2bintegration.B2bGlobalParameters;
import org.openbravo.b2bintegration.B2bOrgParameters;
import org.openbravo.b2bintegration.base.activitylog.ActivityLogUtils;
import org.openbravo.b2bintegration.base.emailengine.B2BEmailManager;
import org.openbravo.b2bintegration.base.emailengine.EmailInstance;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.utils.FormatUtilities;

/**
 * 
 * 
 */
public class AddVendorPortalPrivilegesToContact extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(AddVendorPortalPrivilegesToContact.class);
  private static B2bGlobalParameters b2bglobalparams;
  private static BusinessPartner SupplyChainManager;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    // Retrieve User

    final String strAdUserId = (String) bundle.getParams().get("AD_User_ID");
    final User user = OBDal.getInstance().get(User.class, strAdUserId);

    try {

      checkparams(user);
      assignRoleAndDefaults(user);
      String password = assignUsernameAndPassword(user);
      user.setB2bIsvendorportaluser(true);
      sendEmails(user, password);

      ActivityLogUtils.log(OBContext.getOBContext().getUser(), "b2b_activitylog_usergrantaccess",
          user);
      // If successful,

      OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("@Success@");
      String messagestring = Utility.messageBD(bundle.getConnection(),
          "b2b_addvendorcontact_success", bundle.getContext().getLanguage());

      messagestring = messagestring.replace("@useremail@", "<i>" + user.getEmail() + "</i>");
      messagestring = messagestring.replace("@vendorSCM@", SupplyChainManager.getIdentifier());

      msg.setMessage(messagestring);
      bundle.setResult(msg);
    } catch (Exception e) {
      OBError msg = new OBError();
      msg.setType("Error");
      msg.setTitle("@Error@");
      msg.setMessage(e.getMessage());
      bundle.setResult(msg);
    } finally {

    }
    // If not successful, show DAL error

  }

  private static B2bGlobalParameters getB2BGlobalParams() throws Exception {
    OBCriteria<B2bGlobalParameters> obc = OBDal.getInstance().createCriteria(
        B2bGlobalParameters.class);

    if (obc.list().isEmpty()) {
      throw new Exception("@b2b_noglobalparameters@");
    }
    return obc.list().get(0);
  }

  private void sendEmails(User user, String password) throws Exception {

    // Send email to actual user
    EmailInstance useremail = new EmailInstance();

    useremail.setTemplate("AddVendorPortalPrivilegesToContact-ToVendor", user.getBusinessPartner()
        .getB2BVendorParametersList().get(0).getLanguageID().getLanguage());

    useremail.addToRecipient(user.getEmail());

    useremail.addSubjectParameter("@B2BPortalName@", b2bglobalparams.getB2bportalname());

    useremail.addBodyParameter("@B2BPortalName@", b2bglobalparams.getB2bportalname());
    useremail.addBodyParameter("@PortalUrl@", b2bglobalparams.getB2bportalurl());
    useremail.addBodyParameter("@Name_of_User@", user.getName());
    useremail.addBodyParameter("@access_username@", user.getEmail());
    useremail.addBodyParameter("@password@", password);
    useremail.addBodyParameter("@SCM_Name@", SupplyChainManager.getIdentifier());

    B2bEmailInstance B2bEmailInstance = useremail.saveAndClose();
    B2BEmailManager.send(B2bEmailInstance);

    // TODO Send email to Vendor's SCM

    EmailInstance scmemail = new EmailInstance();

    scmemail.setTemplate("AddVendorPortalPrivilegesToContact-ToSCM", "en_US");

    scmemail.addToRecipient(SupplyChainManager.getADUserList().get(0).getEmail());

    scmemail.addSubjectParameter("@Name_of_User@", user.getName());
    scmemail.addSubjectParameter("@B2BPortalName@", b2bglobalparams.getB2bportalname());

    scmemail.addBodyParameter("@B2BPortalName@", b2bglobalparams.getB2bportalname());
    scmemail.addBodyParameter("@Name_of_SCM@", SupplyChainManager.getIdentifier());
    scmemail.addBodyParameter("@BusinessPartner@", user.getBusinessPartner().getIdentifier());
    scmemail.addBodyParameter("@Name_of_User@", user.getName());
    scmemail.addBodyParameter("@Email_of_User@", user.getEmail());

    B2bEmailInstance B2bEmailInstance2 = scmemail.saveAndClose();
    B2BEmailManager.send(B2bEmailInstance2);

  }

  private String assignUsernameAndPassword(User user) throws Exception {
    user.setUsername(user.getEmail());
    String password = RandomStringUtils.randomAlphanumeric(8);
    user.setPassword(FormatUtilities.sha1Base64(password));

    OBDal.getInstance().flush();
    return password;
  }

  private void assignRoleAndDefaults(User user) throws Exception {
    // Assign default role for this user.

    // Query the database for the B2B Organization Parameters record, to get the defaultrole
    OBCriteria<B2bOrgParameters> obc = OBDal.getInstance().createCriteria(B2bOrgParameters.class);
    obc.add(Restrictions.eq(B2bOrgParameters.PROPERTY_ORGANIZATION, user.getBusinessPartner()
        .getOrganization()));

    if (obc.list().isEmpty()) {
      throw new Exception("@b2b_addvendorcontact_orgisnotconfigured@");
    }

    Role defaultRoleForContact = obc.list().get(0).getDefaultVendorrole();

    // Create a new "User Roles" object for this user

    final UserRoles newUserRole = OBProvider.getInstance().get(UserRoles.class);
    newUserRole.setRole(defaultRoleForContact);
    newUserRole.setUserContact(user);
    newUserRole.setRoleAdmin(false);
    OBDal.getInstance().save(newUserRole);
    OBDal.getInstance().flush();

    // Set up default Role, etc.
    user.setDefaultClient(user.getClient());
    user.setDefaultOrganization(user.getBusinessPartner().getOrganization());
    user.setDefaultRole(defaultRoleForContact);
    // user.setDefaultLanguage(null);
    OBDal.getInstance().flush();

  }

  private void checkparams(User user) throws Exception {
    // -- User has a BusinessPartner (should have one, since this button is only accessed in the
    // Contact tab within Business Partner window)

    if (user.getBusinessPartner() == null)
      throw new Exception("@b2b_addvendorcontact_noBPfound@");

    // -- The BusinessPartner is a Vendor, is B2B enabled, and has a B2B Supply Chain Manager
    BusinessPartner bp = user.getBusinessPartner();

    if (!bp.isVendor()) {
      throw new Exception("@b2b_addvendorcontact_BPisnotvendor@");
    }

    if (bp.getB2BVendorParametersList().isEmpty()) {
      throw new Exception("@b2b_addvendorcontact_Vendorisnotb2benabled@");
    }

    // -- User has an email
    if (user.getEmail() == null || user.getEmail().equals("")) {
      throw new Exception("@b2b_addvendorcontact_userhasnotemail@");
    }
    b2bglobalparams = getB2BGlobalParams();
    SupplyChainManager = user.getBusinessPartner().getB2BVendorParametersList().get(0)
        .getDefaultVendorScm();
  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

  }

}
