/*******************************************************************************
 * Copyright 2016, 2018 elasql.org contributors
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
package org.elasql.bench.server.procedure.calvin.micro;

import java.util.HashMap;
import java.util.Map;


import org.vanilladb.bench.server.param.micro.MicroTxnProcParamHelper;
import org.vanilladb.core.sql.Constant;
import org.vanilladb.core.sql.DoubleConstant;
import org.vanilladb.core.sql.IntegerConstant;

public class MicroTxnProc extends CalvinStoredProcedure<MicroTxnProcParamHelper> {

	private Map<RecKey, Constant> writeConstantMap = new HashMap<RecKey, Constant>();

	public MicroTxnProc(long txNum) {
		super(txNum, new MicroTxnProcParamHelper());
	}

	@Override
	public void prepareKeys() {
		// set read keys
		for (int idx = 0; idx < paramHelper.getReadCount(); idx++) {
			int iid = paramHelper.getReadItemId(idx);
			
			// create record key for reading
			Map<String, Constant> keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("i_id", new IntegerConstant(iid));
			RecKey key = new RecKey("item", keyEntryMap);
			String sql = "SELECT i_name, i_price FROM item WHERE i_id = " + iid;

			addReadKey(key, sql);
		}

		// set write keys
		for (int idx = 0; idx < paramHelper.getWriteCount(); idx++) {
			int iid = paramHelper.getWriteItemId(idx);
			double newPrice = paramHelper.getNewItemPrice(idx);
			
			// create record key for writing
			Map<String, Constant> keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("i_id", new IntegerConstant(iid));
			RecKey key = new RecKey("item", keyEntryMap);
			addWriteKey(key);

			// Create key-value pairs for writing
			Constant c = new DoubleConstant(newPrice);
			writeConstantMap.put(key, c);
		}
	}

	@Override
	protected void executeSql(Map<RecKey, CalvinRecord> reads) {
//		// SELECT i_name, i_price FROM items WHERE i_id = ...
//		int idx = 0;
//		for (CachedRecord rec : readings.values()) {
//			paramHelper.setItemName((String) rec.getVal("i_name").asJavaVal(), idx);
//			paramHelper.setItemPrice((double) rec.getVal("i_price").asJavaVal(), idx++);
//		}
//
//		// UPDATE items SET i_price = ... WHERE i_id = ...
//		for (Map.Entry<RecordKey, Constant> pair : writeConstantMap.entrySet()) {
//			CachedRecord rec = readings.get(pair.getKey());
//			rec.setVal("i_price", pair.getValue());
//			update(pair.getKey(), rec);
//		}
		
		// set read keys
//		for (int idx = 0; idx < paramHelper.getReadCount(); idx++) {
//			int iid = paramHelper.getReadItemId(idx);
//			
//			// create record key for reading
//			Map<String, Constant> keyEntryMap = new HashMap<String, Constant>();
//			keyEntryMap.put("i_id", new IntegerConstant(iid));
//			RecordKey key = new RecordKey("item", keyEntryMap);
//			addReadKey(key);
//			String sql = "SELECT i_name, i_price FROM item WHERE i_id = " + iid;
//			CalvinStoredProcedure.
//		}

		// set write keys
		for (int idx = 0; idx < paramHelper.getWriteCount(); idx++) {
			int iid = paramHelper.getWriteItemId(idx);
			double newPrice = paramHelper.getNewItemPrice(idx);
			
			// create record key for writing
			Map<String, Constant> keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("i_id", new IntegerConstant(iid));
			RecKey key = new RecKey("item", keyEntryMap);			String sql = "UPDATE item SET i_price = " + newPrice + " WHERE i_id =" + iid;

			
//			addWriteKey(key);

			// Create key-value pairs for writing
//			Constant c = new DoubleConstant(newPrice);
//			writeConstantMap.put(key, c);
			String sql = "UPDATE item SET i_price = " + newPrice + " WHERE i_id =" + iid;
			MicroTxnProc.update(key, sql);
		}

	}
}
