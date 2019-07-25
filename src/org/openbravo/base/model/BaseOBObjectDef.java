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
 * All portions are Copyright (C) 2008-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.model;

/**
 * Defines the BaseOBObject interface, this interface has been introduced to prevent cyclic
 * references between generated code and model code which is used by the generator. This interface
 * only contains the minimally required methods to prevent this cycle.
 * 
 * @author mtaal
 */

public interface BaseOBObjectDef {

  public Object get(String propertyName);

  public void set(String propertyName, Object value);

  public Entity getEntity();

  public Object getId();

  public String getIdentifier();
}
