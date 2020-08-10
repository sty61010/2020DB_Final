/*******************************************************************************
 * Copyright 2016, 2018 vanilladb.org contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.vanilladb.bench.rte;

import java.sql.Connection;
import java.sql.SQLException;

import org.vanilladb.bench.BenchmarkerParameters;
import org.vanilladb.bench.BenchTransactionType;
import org.vanilladb.bench.TxnResultSet;
import org.vanilladb.bench.remote.SutConnection;
import org.vanilladb.bench.remote.SutResultSet;
import org.vanilladb.bench.rte.jdbc.JdbcExecutor;
import org.vanilladb.bench.util.BenchProperties;

public abstract class TransactionExecutor<T extends BenchTransactionType> {

	public static final boolean DISPLAY_RESULT;

	static {
		DISPLAY_RESULT = BenchProperties.getLoader()
				.getPropertyAsBoolean(TransactionExecutor.class.getName() + ".DISPLAY_RESULT", false);
	}

	protected TxParamGenerator<T> pg;

	public abstract TxnResultSet execute(SutConnection conn, int rteID);
	
	protected abstract JdbcExecutor<T> getJdbcExecutor();
	
	protected SutResultSet executeTxn(SutConnection conn, Object[] pars, int rteID) throws SQLException {
		SutResultSet result = null;
		
		switch (BenchmarkerParameters.CONNECTION_MODE) {
		case JDBC:
			Connection jdbcConn = conn.toJdbcConnection();
			jdbcConn.setAutoCommit(false);
			result = getJdbcExecutor().execute(jdbcConn, pg.getTxnType(), pars);
			break;
		case SP:
			result = conn.callStoredProc(rteID, pg.getTxnType().getProcedureId(), pars);
			break;
		}
		
		return result;
	}
}
