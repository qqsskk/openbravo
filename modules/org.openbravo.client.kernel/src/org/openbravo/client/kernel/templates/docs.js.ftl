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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/

(function(w) {
  var d = w.document, ul, li, a;
  ul = d.getElementById('js-files');

  if(ul === null) {
    return;
  }

<#list data.documentationResources as doc>
  li = d.createElement('li');
  a = d.createElement('a');
  a.setAttribute('href', '${doc}');
  a.appendChild(d.createTextNode('${doc}'));
  li.appendChild(a);
  ul.appendChild(li);
</#list>
})(window);