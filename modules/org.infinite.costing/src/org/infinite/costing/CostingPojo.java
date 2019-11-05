package org.infinite.costing;

import java.math.BigDecimal;
import java.util.Date;

import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

public class CostingPojo {
	private final Organization organization;
	private final BigDecimal qty;
	private final BigDecimal cummulativeQty;
	private final BigDecimal unitCost;
	private final BigDecimal cummulativeCost;
	private final BigDecimal unitPrice;
	private final MaterialTransaction materialTransaction;
	private final Date startDate;
	private Date endDate;
	private final Warehouse warehouse;
	private final String costType;
	private final boolean isPermanent;
	private final boolean isInsertNewCostingRecord;

	public CostingPojo(Organization organization, BigDecimal qty, BigDecimal cummulativeQty, BigDecimal unitCost,
			BigDecimal cummulativeCost, BigDecimal unitPrice, MaterialTransaction materialTransaction, Date startDate,
			Date endDate,
			Warehouse warehouse, String costType, boolean isPermanent, boolean isInsertNewCostingRecord) {
		super();
		this.organization = organization;
		this.qty = qty;
		this.cummulativeQty = cummulativeQty;
		this.unitCost = unitCost;
		this.cummulativeCost = cummulativeCost;
		this.unitPrice = unitPrice;
		this.materialTransaction = materialTransaction;
		this.startDate = startDate;
		this.endDate = endDate;
		this.warehouse = warehouse;
		this.costType = costType;
		this.isPermanent = isPermanent;
		this.isInsertNewCostingRecord = isInsertNewCostingRecord;
	}

	public Organization getOrganization() {
		return organization;
	}

	public BigDecimal getQty() {
		return qty;
	}

	public BigDecimal getCummulativeQty() {
		return cummulativeQty;
	}

	public BigDecimal getUnitCost() {
		return unitCost;
	}

	public BigDecimal getCummulativeCost() {
		return cummulativeCost;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public MaterialTransaction getMaterialTransaction() {
		return materialTransaction;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public String getCostType() {
		return costType;
	}

	public boolean isPermanent() {
		return isPermanent;
	}

	public boolean isInsertNewCostingRecord() {
		return isInsertNewCostingRecord;
	}

	public void setEndDate(Date endDate) {
		this.endDate = new Date(endDate.getTime());
	}

}
