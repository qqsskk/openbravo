package org.wirabumi.bprl.ad_callout;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class RequestedQTYCallout extends SimpleCallout {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		String lastChanged = info.getLastFieldChanged();
		if (lastChanged.equals("inpemBprlRequestedqty")){
			BigDecimal requestedQty = info.getBigDecimalParameter("inpemBprlRequestedqty");
			info.addResult("inpmovementqty", requestedQty);
		}

	}

}
