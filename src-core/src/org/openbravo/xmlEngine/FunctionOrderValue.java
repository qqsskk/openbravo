/*
 ************************************************************************************
 * Copyright (C) 2001-2010 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.xmlEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FunctionOrderValue extends FunctionValue {
  int order;

  static Logger log4jFunctionOrderValue = LogManager.getLogger();

  public FunctionOrderValue(FunctionTemplate functionTemplate, XmlDocument xmlDocument) {
    super(functionTemplate, xmlDocument);
  }

  @Override
  public String print() {
    return Integer.toString(order);
  }

  @Override
  public void acumulate() {
    ;
  }

  @Override
  public void init() {
    order++;
  }

}
