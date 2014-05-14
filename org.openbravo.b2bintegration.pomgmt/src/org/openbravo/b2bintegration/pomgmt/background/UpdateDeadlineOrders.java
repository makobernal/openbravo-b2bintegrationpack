package org.openbravo.b2bintegration.pomgmt.background;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.b2bintegration.base.activitylog.ActivityLogUtils;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class UpdateDeadlineOrders extends DalBaseProcess {
  private List<BusinessPartner> b2bpartnerslist;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    try {

      OBCriteria<BusinessPartner> obc = OBDal.getInstance().createCriteria(BusinessPartner.class);
      obc.add(Restrictions.isNotEmpty(BusinessPartner.PROPERTY_B2BVENDORPARAMETERSLIST));
      b2bpartnerslist = obc.list();

      List<Order> openorderslist = GetOpenOrders(get_today());
      UpdateOpenOrders(openorderslist);

    } finally {

    }
  }

  private void UpdateOpenOrders(List<Order> openorderslist) throws Exception {
    for (Order order : openorderslist) {

      StringBuilder newReqChanges = new StringBuilder();

      newReqChanges.append(getDateTimeString(new Date()));
      newReqChanges.append(" - ");
      newReqChanges.append("(Auto) - Acceptance Deadline has expired, Order Rejected");
      newReqChanges.append("\n");
      if (order.getB2bpoScmreqchanges() != null) {
        newReqChanges.append(order.getB2bpoScmreqchanges());
      }

      order.setB2bpoScmreqchanges(newReqChanges.toString());
      order.setB2bpoOrderstatus("OUT");
      OBDal.getInstance().flush();
      ActivityLogUtils.log(OBContext.getOBContext().getUser(),
          "b2bpo_activitylog_orderdeadlinerejected", order);
    }

  }

  private List<Order> GetOpenOrders(Date comparedate) {
    OBCriteria<Order> obc = OBDal.getInstance().createCriteria(Order.class);
    obc.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));
    obc.add(Restrictions.eq(Order.PROPERTY_PROCESSED, false));
    obc.add(Restrictions.not(Restrictions.eq(Order.PROPERTY_B2BPOORDERSTATUS, "N/A")));
    obc.add(Restrictions.not(Restrictions.eq(Order.PROPERTY_B2BPOORDERSTATUS, "OK")));
    obc.add(Restrictions.not(Restrictions.eq(Order.PROPERTY_B2BPOORDERSTATUS, "OUT")));
    obc.add(Restrictions.le(Order.PROPERTY_B2BPOACCEPTANCEDEADLINE, comparedate));
    obc.add(Restrictions.in(Order.PROPERTY_BUSINESSPARTNER, b2bpartnerslist));

    return obc.list();
  }

  private Date get_today() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  private static String getDateTimeString(Date date) {
    try {
      String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
          "dateTimeFormat.java");
      SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
      return (outputFormat.format(date));
    } catch (Exception e) {

      return null;
    }
  }

}