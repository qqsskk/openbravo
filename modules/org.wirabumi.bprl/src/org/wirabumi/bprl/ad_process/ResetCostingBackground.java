package org.wirabumi.bprl.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

public class ResetCostingBackground extends DalBaseProcess {
	
	private ProcessLogger logger;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		
		logger = bundle.getLogger();
		
		// reset transction cost menjadi not calcualated
		// hanya berlaku untuk produk dengan cost type selain STA (standard costing)
		// khusus untuk costing record yang diset permanent=Y maka tidak akan direset
		
		// proses reset akan mengahpus costing dan transaction cost, serta mengembalikan material transaction
		// sebagaimana record yang belum dihitung transaction cost nya.
		
		String clientID = OBContext.getOBContext().getCurrentClient().getId();
		
		deleteFIFOTransaction(clientID);
		deleteCosting(clientID);
		deleteTransactionCost(clientID);
		resetMaterialTransactionCost(clientID);
		

	}

	private void resetMaterialTransactionCost(String clientID) {
		String sql = "update m_transaction"
				+ " set transactioncost=null, m_costing_algorithm_id=null,"
				+ " iscostcalculated='N', c_currency_id=null, costing_status='NC', isprocessed='N'"
				+ " where ad_client_id=?";
		ConnectionProvider conn = new DalConnectionProvider();
		Connection connection;
		try {
			connection = conn.getConnection();
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, clientID);
			int result = ps.executeUpdate();
			logger.log(result+" material transaction record(s) reseted"+System.getProperty("line.separator"));
			
			
		} catch (NoConnectionAvailableException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	private void deleteTransactionCost(String clientID) {
		String sql = "delete from m_transaction_cost where ad_client_id=?";
		ConnectionProvider conn = new DalConnectionProvider();
		Connection connection;
		try {
			connection = conn.getConnection();
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, clientID);
			int result = ps.executeUpdate();
			logger.log(result+" transaction cost record(s) deleted"+System.getProperty("line.separator"));
		} catch (NoConnectionAvailableException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	private void deleteCosting(String clientID) {
		String sql = "delete from m_costing where ad_client_id=?"
				+ " and ispermanent='N' and costtype not in ('STA')";
		ConnectionProvider conn = new DalConnectionProvider();
		Connection connection;
		try {
			connection = conn.getConnection();
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, clientID);
			int result = ps.executeUpdate();
			logger.log(result+" costing record(s) deleted"+System.getProperty("line.separator"));
		} catch (NoConnectionAvailableException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	private void deleteFIFOTransaction(String clientID) {
		String sql = "delete from bprl_fifotransaction where ad_client_id=?";
		ConnectionProvider conn = new DalConnectionProvider();
		Connection connection;
		try {
			connection = conn.getConnection();
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, clientID);
			int result = ps.executeUpdate();
			logger.log(result+" FIFO transaction record(s) deleted"+System.getProperty("line.separator"));
		} catch (NoConnectionAvailableException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

}
