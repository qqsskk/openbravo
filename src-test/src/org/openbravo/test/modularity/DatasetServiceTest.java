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
 * All portions are Copyright (C) 2008-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.modularity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.ModulesInOB3Distribution;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.DataSet;
import org.openbravo.model.ad.utility.DataSetColumn;
import org.openbravo.model.ad.utility.DataSetTable;
import org.openbravo.service.dataset.DataSetService;
import org.openbravo.service.db.DataExportService;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests the {@link DataSetService} object and its methods.
 * 
 * @author mtaal
 */

public class DatasetServiceTest extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();

  /**
   * Tests that all data sets have correct queries defined in the DataSetTable.
   * 
   * @see DataSet#getDataSetTableList()
   * @see DataSetTable#getSQLWhereClause()
   */
  @Test
  public void testCheckQueries() {
    setTestAdminContext();

    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("ClientID", "0");

    final List<DataSet> dss = getDatasetsOfModulesInOb3Distribution();
    setSystemAdministratorContext();
    for (final DataSet ds : dss) {
      if (!ds.getName().equalsIgnoreCase("AD") && !ds.getName().equalsIgnoreCase("ADRD")) {
        for (final DataSetTable dt : ds.getDataSetTableList()) {
          try {
            // just test but do nothing with return value
            DataSetService.getInstance().getExportableObjects(dt, "0", parameters);
          } catch (final Exception e) {
            log.debug(ds.getName() + ": " + dt.getEntityName() + ": " + e.getMessage());
          }
        }
      }
    }
  }

  /**
   * Exports the data of all data sets.
   */
  @Test
  public void testExportAllDataSets() {
    setTestAdminContext();
    final List<DataSet> dss = getDatasetsOfModulesInOb3Distribution();
    setSystemAdministratorContext();

    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("ClientID", "0");

    for (final DataSet ds : dss) {
      if (!ds.getName().equalsIgnoreCase("AD") && !ds.getName().equalsIgnoreCase("ADRD")) {
        final String xml = DataExportService.getInstance().exportDataSetToXML(ds, "0", parameters);
        log.debug(xml);
      }
    }
  }

  /**
   * Creates a whereclause and tests it using a {@link DataSetTable}.
   * 
   * @see Table
   */
  @Test
  public void testDataSetTable() {
    setSystemAdministratorContext();
    final DataSetTable dst = OBProvider.getInstance().get(DataSetTable.class);
    final Table t = OBProvider.getInstance().get(Table.class);
    t.setName("ADTable");
    dst.setTable(t);
    dst.setSQLWhereClause("(" + Table.PROPERTY_DELETABLERECORDS + "='N' or " + Table.PROPERTY_VIEW
        + "='N') and client.id='0'");
    final List<BaseOBObject> l = DataSetService.getInstance().getExportableObjects(dst, "0");
    for (final BaseOBObject bob : l) {
      log.debug(bob.getIdentifier());
    }
  }

  /**
   * Read all data defined for all data sets.
   */
  @Test
  public void testReadAll() {
    setSystemAdministratorContext();

    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("ClientID", "0");

    final DataSetService dss = DataSetService.getInstance();
    final List<DataSet> ds = getDatasetsOfModulesInOb3Distribution();
    for (final DataSet d : ds) {
      if (!d.getName().equalsIgnoreCase("AD") && !d.getName().equalsIgnoreCase("ADRD")) {
        log.debug("Exporting DataSet: " + d.getName());
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        final List<DataSetTable> dts = d.getDataSetTableList();
        for (final DataSetTable dt : dts) {
          log.debug("Exporting DataSetTable: " + dt.getTable().getName());
          final List<DataSetColumn> dcs = dt.getDataSetColumnList();

          final List<BaseOBObject> bobs = dss.getExportableObjects(dt, "0", parameters);
          for (final BaseOBObject bob : bobs) {
            final List<Property> ps = dss.getExportableProperties(bob, dt, dcs);
            final StringBuilder sb = new StringBuilder();
            sb.append(bob.getIdentifier() + " has " + ps.size() + " properties to export");
          }
        }
      }
    }
  }

  private List<DataSet> getDatasetsOfModulesInOb3Distribution() {
    OBQuery<DataSet> query = OBDal.getInstance()
        .createQuery(DataSet.class, " as e where e.module.id in (:moduleList)");
    query.setNamedParameter("moduleList", ModulesInOB3Distribution.getModules());
    return query.list();
  }
}
