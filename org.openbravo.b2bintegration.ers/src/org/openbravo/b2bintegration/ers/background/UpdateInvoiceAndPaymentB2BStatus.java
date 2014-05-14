package org.openbravo.b2bintegration.ers.background;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class UpdateInvoiceAndPaymentB2BStatus extends DalBaseProcess {
  private List<BusinessPartner> b2bpartnerslist;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    try {

      OBCriteria<BusinessPartner> obc = OBDal.getInstance().createCriteria(BusinessPartner.class);
      obc.add(Restrictions.isNotEmpty(BusinessPartner.PROPERTY_B2BVENDORPARAMETERSLIST));
      b2bpartnerslist = obc.list();

      SetPendingInvoices();
      SetAutoInvoices();
      SetPendingPayments();
      SetAutoPayments();

    } finally {

    }
  }

  private void SetAutoPayments() {
    OBCriteria<FIN_Payment> obc = OBDal.getInstance().createCriteria(FIN_Payment.class);
    obc.add(Restrictions.eq(FIN_Payment.PROPERTY_RECEIPT, false));
    obc.add(Restrictions.eq(FIN_Payment.PROPERTY_PROCESSED, true));
    obc.add(Restrictions.eq(FIN_Payment.PROPERTY_B2BERSACKSTATUS, "P"));

    for (FIN_Payment payment : obc.list()) {
      if (payment.getBusinessPartner().getB2BVendorParametersList().get(0).isAutoackpayments()) {
        payment.setB2bersAckstatus("AUTO");
      }
    }
    OBDal.getInstance().flush();
  }

  private void SetPendingPayments() {
    OBCriteria<FIN_Payment> obc = OBDal.getInstance().createCriteria(FIN_Payment.class);
    obc.add(Restrictions.in(FIN_Payment.PROPERTY_BUSINESSPARTNER, b2bpartnerslist));
    obc.add(Restrictions.eq(FIN_Payment.PROPERTY_RECEIPT, false));
    obc.add(Restrictions.eq(FIN_Payment.PROPERTY_PROCESSED, true));
    obc.add(Restrictions.eq(FIN_Payment.PROPERTY_B2BERSACKSTATUS, "N/A"));

    for (FIN_Payment payment : obc.list()) {
      payment.setB2bersAckstatus("P");
    }
    OBDal.getInstance().flush();
  }

  private void SetAutoInvoices() {
    OBCriteria<Invoice> obc = OBDal.getInstance().createCriteria(Invoice.class);
    obc.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, false));
    obc.add(Restrictions.eq(Invoice.PROPERTY_PROCESSED, true));
    obc.add(Restrictions.eq(Invoice.PROPERTY_B2BERSVALIDATIONSTATUS, "P"));

    for (Invoice invoice : obc.list()) {
      if (invoice.getBusinessPartner().getB2BVendorParametersList().get(0).isAutovalidateinvoice()) {
        Long autovalidatedays = invoice.getBusinessPartner().getB2BVendorParametersList().get(0)
            .getAutovalidateDays();
        if (autovalidatedays == 0 || pendingValidationExpired(invoice, autovalidatedays)) {
          invoice.setB2bersValidationstatus("AUTO");
          invoice.setB2bersPendingvaliddate(get_today());
        }

      }
    }
    OBDal.getInstance().flush();
  }

  private boolean pendingValidationExpired(Invoice invoice, Long autovalidatedays) {
    Calendar today = Calendar.getInstance();
    today.setTime(get_today());

    Calendar expirydate = Calendar.getInstance();
    expirydate.setTime(get_today());
    expirydate.add(Calendar.DATE, autovalidatedays.intValue());

    return expirydate.before(today);
  }

  private void SetPendingInvoices() {
    OBCriteria<Invoice> obc = OBDal.getInstance().createCriteria(Invoice.class);
    obc.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, false));
    obc.add(Restrictions.eq(Invoice.PROPERTY_PROCESSED, true));
    obc.add(Restrictions.eq(Invoice.PROPERTY_B2BERSVALIDATIONSTATUS, "N/A"));
    obc.add(Restrictions.in(Invoice.PROPERTY_BUSINESSPARTNER, b2bpartnerslist));

    for (Invoice invoice : obc.list()) {

      invoice.setB2bersValidationstatus("P");
      invoice.setB2bersPendingvaliddate(get_today());
    }
    OBDal.getInstance().flush();
  }

  private Date get_today() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

}