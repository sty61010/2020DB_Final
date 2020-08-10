/*******************************************************************************
 * Copyright 2016, 2017 vanilladb.org contributors
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
package org.vanilladb.bench.server.procedure;

import java.util.Map;

import org.calvin.cache.CalvinRecord;
import org.calvin.sql.RecKey;
import org.calvin.storedprocedures.CalvinStoredProcedure;
import org.vanilladb.core.server.VanillaDb;
import org.vanilladb.core.sql.storedprocedure.StoredProcedure;
import org.vanilladb.core.sql.storedprocedure.StoredProcedureParamHelper;

public class StopProfilingProc extends CalvinStoredProcedure<StoredProcedureParamHelper> {

	public StopProfilingProc() {
		super(StoredProcedureParamHelper.newDefaultParamHelper());
	}

	@Override
	protected void executeSql() {
		VanillaDb.stopProfilerAndReport();
	}

	@Override
	protected void prepareRecKey() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void executeSql(Map<RecKey, CalvinRecord> reads) {
		// TODO Auto-generated method stub
		
	}

}

