/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2015-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;

class ProductServiceConfigurationObserver extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Product.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkConfiguration(event);
  }

  private void checkConfiguration(EntityUpdateEvent event) {
    final Entity productEntity = ModelProvider.getInstance().getEntity(Product.ENTITY_NAME);
    final Property linkedToProductProperty = productEntity
        .getProperty(Product.PROPERTY_LINKEDTOPRODUCT);
    final Property qtyRuleProperty = productEntity.getProperty(Product.PROPERTY_QUANTITYRULE);
    final Boolean linkedToProduct = (Boolean) event.getCurrentState(linkedToProductProperty);
    String qtyRule = (String) event.getCurrentState(qtyRuleProperty);
    final Boolean linkedToProductPrevious = (Boolean) event
        .getPreviousState(linkedToProductProperty);
    String qtyRulePrevious = (String) event.getPreviousState(qtyRuleProperty);
    if (qtyRulePrevious == null) {
      qtyRulePrevious = "";
    }
    if (qtyRule == null) {
      qtyRule = "";
    }
    final Product product = OBDal.getInstance().get(Product.class, (String) event.getId());
    if (linkedToProductPrevious != linkedToProduct || !qtyRulePrevious.equals(qtyRule)) {
      checkNotDeliveredOrders(product);
    }
  }

  private void checkNotDeliveredOrders(Product product) {
    StringBuilder hql = new StringBuilder();
    hql.append("as ol ");
    hql.append("join ol." + OrderLine.PROPERTY_SALESORDER + " as o ");
    hql.append("where o." + Order.PROPERTY_DOCUMENTSTATUS);
    hql.append(" = 'CO' ");
    hql.append("and ol." + OrderLine.PROPERTY_PRODUCT);
    hql.append(" = :product ");
    hql.append("and ol." + OrderLine.PROPERTY_DELIVEREDQUANTITY + "<> ol."
        + OrderLine.PROPERTY_ORDEREDQUANTITY);
    OBQuery<OrderLine> notDeliveredOrderLineQuery = OBDal.getInstance()
        .createQuery(OrderLine.class, hql.toString());
    notDeliveredOrderLineQuery.setNamedParameter("product", product);
    notDeliveredOrderLineQuery.setMaxResult(1);
    OrderLine notDeliveredOrderLine = notDeliveredOrderLineQuery.uniqueResult();
    if (notDeliveredOrderLine != null) {
      String[] params = { notDeliveredOrderLine.getSalesOrder().getDocumentNo() };
      throw new OBException(OBMessageUtils.getI18NMessage("ServiceCannotBeModified", params));
    }
  }

}
