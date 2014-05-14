package org.openbravo.b2bintegration.base.emailengine;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.b2bintegration.B2bEmailInstance;
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
public class ResendEmailButton extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(ResendEmailButton.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String debug = (String) bundle.getParams().get("debug");

    Boolean isdebug = debug.equals("Y") ? true : false;

    // Retrieve EmailInstance

    final String strB2bEmailInstanceId = (String) bundle.getParams().get("B2B_Emailinstance_ID");
    final B2bEmailInstance B2bEmailInstance = OBDal.getInstance().get(B2bEmailInstance.class,
        strB2bEmailInstanceId);

    try {

      B2BEmailManager.send(B2bEmailInstance, isdebug);
      // If successful,

      OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("@Success@");
      String messagestring = Utility.messageBD(bundle.getConnection(), "@b2b_resendemail_success@",
          bundle.getContext().getLanguage());

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
