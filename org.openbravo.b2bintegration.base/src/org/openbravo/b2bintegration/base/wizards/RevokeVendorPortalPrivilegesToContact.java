package org.openbravo.b2bintegration.base.wizards;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.b2bintegration.base.activitylog.ActivityLogUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * This process creates a standard translation module for the current module.
 * 
 */
public class RevokeVendorPortalPrivilegesToContact extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(AddVendorPortalPrivilegesToContact.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    // Retrieve User

    final String strAdUserId = (String) bundle.getParams().get("AD_User_ID");
    final User user = OBDal.getInstance().get(User.class, strAdUserId);

    try {

      RevokeRoleAndDefaults(user);
      DropUsernameAndPassword(user);
      user.setB2bIsvendorportaluser(false);

      // If successful,

      ActivityLogUtils.log(OBContext.getOBContext().getUser(), "b2b_activitylog_userrevokeaccess",
          user);

      OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("@Success@");
      String messagestring = Utility.messageBD(bundle.getConnection(),
          "@b2b_revokevendorcontact_success@", bundle.getContext().getLanguage());

      // messagestring = messagestring.replace("@useremail@", user.getEmail());

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

  private void RevokeRoleAndDefaults(User user) {

    List<UserRoles> list = user.getADUserRolesList();
    if (!list.isEmpty()) {

      // for (UserRoles userrole : list) {
      // if (userrole.getRole().isB2BIsVendorB2BContact()) {
      //
      // list.remove(userrole);
      // OBDal.getInstance().remove(userrole);
      // }
      //
      // }
      for (Iterator<UserRoles> iter = list.iterator(); iter.hasNext();) {
        UserRoles userrole = iter.next();
        if (userrole.getRole().isB2BIsVendorB2BContact()) {

          iter.remove();
          OBDal.getInstance().remove(userrole);
        }

      }
    }
    OBDal.getInstance().flush();
  }

  private void DropUsernameAndPassword(User user) {
    user.setUsername(null);
    user.setPassword(null);
    OBDal.getInstance().flush();

  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

  }

}
