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

package org.openbravo.task;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.openbravo.utils.ServerConnection;

public class CheckX extends Task {
  static Logger log4j = LogManager.getLogger();

  @Override
  public void execute() throws BuildException {
    final File f = new File("src-util/diagnostic/build.xml");
    final String fileName = f.getAbsolutePath();
    log4j.info("Checking tomcat's user has X...");
    final String result = new ServerConnection().getCheck("ant", "&file=" + fileName
        + "&task=compile.web");
    if (result.equals("OK"))
      log4j.info("Tomcat's user X. OK");
    else {
      if (result.contains("X11"))
        throw new BuildException("Tomcat's user do not have X. Tip try executing xhost+");
      else
        throw new BuildException(result);
    }
  }
}
