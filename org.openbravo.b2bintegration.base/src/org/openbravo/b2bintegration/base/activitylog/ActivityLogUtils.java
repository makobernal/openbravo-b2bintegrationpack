package org.openbravo.b2bintegration.base.activitylog;

import java.util.Date;

import org.hibernate.criterion.Restrictions;
import org.openbravo.b2bintegration.B2bActivityLog;
import org.openbravo.b2bintegration.B2bEmailInstance;
import org.openbravo.b2bintegration.B2bGlobalParameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;

public class ActivityLogUtils {
  private static B2bGlobalParameters b2bglobalparams;
  private static OBContext context = OBContext.getOBContext();

  public static void log(User who, String what) throws Exception {
    log(who, what, null);
  }

  public static void log(User who, String what, BaseOBObject where) throws Exception {
    log(who, what, where, null);
  }

  public static void log(User who, String what, B2bEmailInstance sentemail) throws Exception {
    log(who, what, null, sentemail);
  }

  public static void log(User who, String what, BaseOBObject where, B2bEmailInstance sentemail)
      throws Exception {

    OBContext.setAdminMode();

    b2bglobalparams = getB2BGlobalParams();

    final B2bActivityLog b2bactivitylog = OBProvider.getInstance().get(B2bActivityLog.class);

    b2bactivitylog.setActivitydate(new Date());
    b2bactivitylog.setUserContact(who);
    b2bactivitylog.setActivitytext(Utility.messageBD(new DalConnectionProvider(), what, context
        .getLanguage().getLanguage()));

    OBDal.getInstance().save(b2bactivitylog);

    if (sentemail != null) {
      b2bactivitylog.setSentEmailInstance(sentemail);
    }

    if (where != null) {
      b2bactivitylog.setRecord((String) where.getId());
      final Table table = OBDal.getInstance().get(Table.class, where.getEntity().getTableId());

      Window window;
      if (table.getPOWindow() == null) {
        window = table.getWindow();
      } else
        window = table.getPOWindow();

      OBCriteria<Tab> obc = OBDal.getInstance().createCriteria(Tab.class);
      obc.add(Restrictions.eq(Tab.PROPERTY_WINDOW, window));
      obc.add(Restrictions.eq(Tab.PROPERTY_TABLEVEL, new Long(0)));

      Tab SCMtab = obc.list().get(0);
      b2bactivitylog.setTab(SCMtab);
      b2bactivitylog.setURL(b2bglobalparams.getB2bportalurl() + "?tabId=" + SCMtab.getId()
          + "&recordId=" + where.getId());

      Tab B2Btab;
      if (table.getB2bB2btab() == null) {
        B2Btab = SCMtab;

      } else {
        B2Btab = table.getB2bB2btab();
      }

      b2bactivitylog.setB2btab(B2Btab);
      b2bactivitylog.setURL(b2bglobalparams.getB2bportalurl() + "?tabId=" + B2Btab.getId()
          + "&recordId=" + where.getId());
    }

    OBDal.getInstance().flush();
    OBContext.restorePreviousMode();
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
