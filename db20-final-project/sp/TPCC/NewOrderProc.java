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
package org.vanilladb.bench.server.procedure.tpcc;

import java.util.HashMap;
import java.util.Map;

import org.calvin.cache.CalvinRecord;
import org.calvin.sql.RecKey;
import org.calvin.storedprocedures.CalvinStoredProcedure;

import org.vanilladb.bench.benchmarks.tpcc.TpccConstants;
import org.vanilladb.bench.server.param.tpcc.NewOrderProcParamHelper;
import org.vanilladb.bench.server.procedure.StoredProcedureHelper;
import org.vanilladb.core.query.algebra.Scan;
import org.vanilladb.core.sql.Constant;
import org.vanilladb.core.sql.IntegerConstant;
import org.vanilladb.core.sql.storedprocedure.StoredProcedure;
import org.vanilladb.core.storage.tx.Transaction;

public class NewOrderProc extends CalvinStoredProcedure<NewOrderProcParamHelper> {

	public NewOrderProc() {
		super(new NewOrderProcParamHelper());
	}
	private static int[] distrOIds;
	static {
		distrOIds = new int[TpccConstants.NUM_WAREHOUSES * TpccConstants.DISTRICTS_PER_WAREHOUSE + 100];
		for (int i = 0; i < distrOIds.length; i++)
			distrOIds[i] = 3001;
	}
	private int fakeOid;

	// Record keys for retrieving data
	private RecKey warehouseKey, districtKey, customerKey;
	private RecKey orderKey, newOrderKey;
	// a {itemKey, stockKey, orderLineKey} per order line
	private RecKey[][] orderLineKeys = new RecKey[15][3];

	// SQL Constants
	Constant widCon, didCon, cidCon, oidCon;

	@Override
	protected void prepareRecKey() {
		Map<String, Constant> keyEntryMap = null;

		// Construct constant from parameters
		widCon = new IntegerConstant(paramHelper.getWid());
		didCon = new IntegerConstant(paramHelper.getDid());
		cidCon = new IntegerConstant(paramHelper.getCid());

		// hard code the next order id
		// TODO: This should be retrieve from district table
		int index = (paramHelper.getWid() - 1) * 10 + paramHelper.getDid() - 1;
		fakeOid = distrOIds[index];
		distrOIds[index] = fakeOid + 1;
		oidCon = new IntegerConstant(fakeOid);

		// =================== Keys for steps 1 ===================
//		NewOrderProcParamHelper paramHelper = getParamHelper();
		Transaction tx = getTransaction();
		int wid = paramHelper.getWid();
		int did = paramHelper.getDid();
		int cid = paramHelper.getCid();
		int nextOid = -1;
		// SELECT ... FROM warehouse WHERE w_id = wid
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("w_id", widCon);
		warehouseKey = new RecKey("warehouse", keyEntryMap);
		String sql = "SELECT w_tax FROM warehouse WHERE w_id = " + wid;
		addReadKey(warehouseKey, sql);

		// SELECT ... FROM district WHERE d_w_id = wid AND d_id = did
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("d_w_id", widCon);
		keyEntryMap.put("d_id", didCon);
		districtKey = new RecKey("district", keyEntryMap);
		sql = "SELECT d_tax, d_next_o_id FROM district WHERE d_w_id = " + wid +
				" AND d_id = " + did;
		addReadKey(districtKey, sql);

		// UPDATE ... WHERE d_w_id = wid AND d_id = did
		sql = "UPDATE district SET d_next_o_id = " + (nextOid + 1) +
				" WHERE d_w_id = " + wid + " AND d_id = " + did;
		addWriteKey(districtKey);

		// SELECT ... WHERE c_w_id = wid AND c_d_id = did AND c_id = cid
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("c_w_id", widCon);
		keyEntryMap.put("c_d_id", didCon);
		keyEntryMap.put("c_id", cidCon);
		customerKey = new RecKey("customer", keyEntryMap);
		sql = "SELECT c_discount, c_last, c_credit FROM customer WHERE c_w_id = " + wid +
				" AND c_d_id = " + did + " AND c_id = " + cid;
		addReadKey(customerKey, sql);

		// INSERT INTO orders (o_id, o_w_id, o_d_id, ...) VALUES (nextOId, wid,
		// did, ...)
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("o_w_id", widCon);
		keyEntryMap.put("o_d_id", didCon);
		keyEntryMap.put("o_id", oidCon);
		orderKey = new RecKey("orders", keyEntryMap);
		addInsertKey(orderKey);

		// INSERT INTO new_order (no_o_id, no_w_id, no_d_id) VALUES
		// (nextOId, wid, did)
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("no_w_id", widCon);
		keyEntryMap.put("no_d_id", didCon);
		keyEntryMap.put("no_o_id", oidCon);
		newOrderKey = new RecKey("new_order", keyEntryMap);
		addInsertKey(newOrderKey);

		// =================== Keys for steps 2 ===================
		int orderLineCount = paramHelper.getOlCount();
		int[][] items = paramHelper.getItems();

		// For each order line
		for (int i = 0; i < orderLineCount; i++) {
//			int olIId = items[i][0];
//			int olSupplyWId = items[i][1];
			int olQuantity = items[i][2];
			// initialize variables
			int olIId = items[i][0];
			int olSupplyWId = items[i][1];
			Constant olIIdCon = new IntegerConstant(olIId);
			Constant supWidCon = new IntegerConstant(olSupplyWId);
			Constant olNumCon = new IntegerConstant(i + 1);

			// SELECT ... FROM item WHERE i_id = olIId
			keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("i_id", olIIdCon);
			orderLineKeys[i][0] = new RecKey("item", keyEntryMap);
			sql = "SELECT i_price, i_name, i_data FROM item WHERE i_id = " + olIId;
			addReadKey(orderLineKeys[i][0], sql);

			// SELECT ... FROM stock WHERE s_i_id = olIId AND s_w_id =
			// olSupplyWId
			keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("s_i_id", olIIdCon);
			keyEntryMap.put("s_w_id", supWidCon);
			orderLineKeys[i][1] = new RecKey("stock", keyEntryMap);
			String sDistXX;
			if (paramHelper.getDid() == 10)
				sDistXX = "s_dist_10";
			else
				sDistXX = "s_dist_0" + paramHelper.getDid();
			
			sql = "SELECT s_quantity, " + sDistXX +
					", s_data, s_ytd, s_order_cnt FROM stock WHERE s_i_id = " + olIId +
					" AND s_w_id = " + olSupplyWId;
			addReadKey(orderLineKeys[i][1], sql);

			// UPDATE ... WHERE s_i_id = olIId AND s_w_id = olSupplyWId
			
			addWriteKey(orderLineKeys[i][1]);

			// INSERT INTO order_line (ol_o_id, ol_w_id, ol_d_id, ol_number,
			// ...)
			// VALUES (nextOId, wid, did, i, ...)
			keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("ol_o_id", oidCon);
			keyEntryMap.put("ol_d_id", didCon);
			keyEntryMap.put("ol_w_id", widCon);
			keyEntryMap.put("ol_number", olNumCon);
			orderLineKeys[i][2] = new RecKey("order_line", keyEntryMap);
			addInsertKey(orderLineKeys[i][2]);
		}
	}
	@Override
	protected void executeSql(Map<RecKey, CalvinRecord> reads) {
		Map<String, Constant> keyEntryMap = null;

		// Construct constant from parameters
		widCon = new IntegerConstant(paramHelper.getWid());
		didCon = new IntegerConstant(paramHelper.getDid());
		cidCon = new IntegerConstant(paramHelper.getCid());

		// hard code the next order id
		// TODO: This should be retrieve from district table
		int index = (paramHelper.getWid() - 1) * 10 + paramHelper.getDid() - 1;
		fakeOid = distrOIds[index];
		distrOIds[index] = fakeOid + 1;
		oidCon = new IntegerConstant(fakeOid);
		// =================== Keys for steps 1 ===================

		NewOrderProcParamHelper paramHelper = getParamHelper();
		Transaction tx = getTransaction();
		int wid = paramHelper.getWid();
		int did = paramHelper.getDid();
		int cid = paramHelper.getCid();
		int nextOid = -1;
		
		// SELECT w_tax FROM warehouse WHERE w_id = wid
		String sql = "SELECT w_tax FROM warehouse WHERE w_id = " + wid;
		Scan s = StoredProcedureHelper.executeQuery(sql, tx);
		s.beforeFirst();
		if (!s.next())
			throw new RuntimeException("Executing '" + sql + "' fails");
		paramHelper.setWTax((Double) s.getVal("w_tax").asJavaVal());
		s.close();

		// SELECT d_tax, d_next_o_id FROM district WHERE d_w_id = wid AND d_id =
		// did
		sql = "SELECT d_tax, d_next_o_id FROM district WHERE d_w_id = " + wid +
				" AND d_id = " + did;
		s = StoredProcedureHelper.executeQuery(sql, tx);
		s.beforeFirst();
		if (!s.next())
			throw new RuntimeException("Executing '" + sql + "' fails");
		nextOid = (Integer) s.getVal("d_next_o_id").asJavaVal();
		paramHelper.setdTax((Double) s.getVal("d_tax").asJavaVal());
		s.close();
		
		if (nextOid < 0)
			throw new RuntimeException("Something wrong with the next_o_id");

		// UPDATE district SET d_next_o_id = (nextOId + 1) WHERE d_w_id = wid
		// AND d_id = did
		sql = "UPDATE district SET d_next_o_id = " + (nextOid + 1) +
				" WHERE d_w_id = " + wid + " AND d_id = " + did;
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("d_w_id", widCon);
		keyEntryMap.put("d_id", didCon);
		districtKey = new RecKey("district", keyEntryMap);
//		StoredProcedureHelper.executeUpdate(sql, tx);
		update(districtKey, sql);

		// SELECT c_discount, c_last, c_credit FROM customer WHERE c_w_id = wid
		// AND c_d_id = did AND c_id = cid
		sql = "SELECT c_discount, c_last, c_credit FROM customer WHERE c_w_id = " + wid +
				" AND c_d_id = " + did + " AND c_id = " + cid;
		s = StoredProcedureHelper.executeQuery(sql, tx);
		s.beforeFirst();
		if (!s.next())
			throw new RuntimeException("Executing '" + sql + "' fails");
		paramHelper.setcDiscount((Double) s.getVal("c_discount").asJavaVal());
		paramHelper.setcLast((String) s.getVal("c_last").asJavaVal());
		paramHelper.setcCredit((String) s.getVal("c_credit").asJavaVal());
		s.close();

		// INSERT INTO orders (o_id, o_w_id, o_d_id, o_c_id, o_entry_d,
		// o_carrier_id, o_ol_cnt, o_all_local) VALUES (nextOId, wid,
		// did, cid, currentTime, 0, olCount, isAllLocal)
		int isAllLocal = paramHelper.isAllLocal() ? 1 : 0;
		long oEntryDate = System.currentTimeMillis();
		int olCount = paramHelper.getOlCount();
		paramHelper.setoEntryDate(oEntryDate);
		
		sql = String.format("INSERT INTO orders (o_id, o_w_id, o_d_id, o_c_id, o_entry_d,"
				+ "o_carrier_id, o_ol_cnt, o_all_local) VALUES (%d, %d," +
				"%d, %d, %d, 0, %d, %d)", nextOid, wid, did, cid, oEntryDate,
				olCount, isAllLocal);
//		StoredProcedureHelper.executeUpdate(sql, tx);
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("o_w_id", widCon);
		keyEntryMap.put("o_d_id", didCon);
		keyEntryMap.put("o_id", oidCon);
		orderKey = new RecKey("orders", keyEntryMap);
		insert(orderKey, sql);
		// INSERT INTO new_order (no_o_id, no_d_id, no_w_id) VALUES
		// (nextOId, did, wid)
		sql = String.format("INSERT INTO new_order (no_o_id, no_d_id, no_w_id) VALUES"
				+ " (%d, %d, %d)", nextOid, did, wid);
		keyEntryMap = new HashMap<String, Constant>();
		keyEntryMap.put("no_w_id", widCon);
		keyEntryMap.put("no_d_id", didCon);
		keyEntryMap.put("no_o_id", oidCon);
		newOrderKey = new RecKey("new_order", keyEntryMap);
//		StoredProcedureHelper.executeUpdate(sql, tx);
		insert(newOrderKey, sql);

		// For each order line
		int totalAmount = 0;
		int[][] items = paramHelper.getItems();
		int orderLineCount = paramHelper.getOlCount();
		for (int i = 0; i < orderLineCount; i++) {
			int olIId = items[i][0];
			int olSupplyWId = items[i][1];
			int olQuantity = items[i][2];
			Constant olIIdCon = new IntegerConstant(olIId);
			Constant supWidCon = new IntegerConstant(olSupplyWId);
			Constant olNumCon = new IntegerConstant(i + 1);
			// SELECT i_price, i_name, i_data FROM item WHERE i_id = olIId
			sql = "SELECT i_price, i_name, i_data FROM item WHERE i_id = " + olIId;
			s = StoredProcedureHelper.executeQuery(sql, tx);
			s.beforeFirst();
			if (!s.next())
				throw new RuntimeException("Executing '" + sql + "' fails");
			// TODO: save i_price, i_name, i_data
			double iPrice = (Double) s.getVal("i_price").asJavaVal();
			s.getVal("i_name").asJavaVal();
			s.getVal("i_data").asJavaVal();
			s.close();

			// SELECT s_quantity, sDistXX, s_data, s_ytd, s_order_cnt FROM
			// stock WHERE s_i_id = olIId AND s_w_id = olSupplyWId
			String sDistXX;
			if (paramHelper.getDid() == 10)
				sDistXX = "s_dist_10";
			else
				sDistXX = "s_dist_0" + paramHelper.getDid();
			
			sql = "SELECT s_quantity, " + sDistXX +
					", s_data, s_ytd, s_order_cnt FROM stock WHERE s_i_id = " + olIId +
					" AND s_w_id = " + olSupplyWId;
			s = StoredProcedureHelper.executeQuery(sql, tx);
			s.beforeFirst();
			if (!s.next())
				throw new RuntimeException("Executing '" + sql + "' fails");
			// TODO: save sDistXX, s_data
			int sQuantity = (Integer) s.getVal("s_quantity").asJavaVal();
			String sDistInfo = (String) s.getVal(sDistXX).asJavaVal();
			s.getVal("s_data").asJavaVal();
			int sYtd = (Integer) s.getVal("s_ytd").asJavaVal();
			int sOrderCnt = (Integer) s.getVal("s_order_cnt").asJavaVal();
			s.close();

			// UPDATE stock SET s_quantity = ..., s_ytd = s_ytd + ol_quantitity,
			// s_order_cnt = s_order_cnt + 1 WHERE s_i_id = olIId AND
			// s_w_id = olSupplyWId
			sQuantity -= olQuantity;
			if (sQuantity < 10)
				sQuantity += 91;
			sYtd += olQuantity;
			sOrderCnt++;
			
			sql = String.format("UPDATE stock SET s_quantity = %d, s_ytd = %d, " +
					"s_order_cnt = %d WHERE s_i_id = %d AND s_w_id = %d",
					sQuantity, sYtd, sOrderCnt, olIId, olSupplyWId);
//			StoredProcedureHelper.executeUpdate(sql, tx);
			keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("s_i_id", olIIdCon);
			keyEntryMap.put("s_w_id", supWidCon);
			orderLineKeys[i][1] = new RecKey("stock", keyEntryMap);
			update(orderLineKeys[i][1], sql);
			
			// INSERT INTO order_line (ol_o_id, ol_d_id, ol_w_id,
			// ol_number,ol_i_id, ol_supply_w_id, ol_delivery_d,
			// ol_quantity, ol_amount, ol_dist_info) VALUES (
			// nextOId, did, wid, i, olIId, olSupplyWid, NULL, olQuantity,
			// DoublePlainPrinter.toPlainString(olAmount), sDistInfo)
			double olAmount = olQuantity * iPrice;
			
			sql = String.format("INSERT INTO order_line (ol_o_id, ol_d_id, ol_w_id, " +
					"ol_number,ol_i_id, ol_supply_w_id, ol_delivery_d, " +
					"ol_quantity, ol_amount, ol_dist_info) VALUES (" +
					"%d, %d, %d, %d, %d, %d, -1, %d, %f, '%s')",
					nextOid, did, wid, i, olIId, olSupplyWId, olQuantity,
					olAmount, sDistInfo);
//			StoredProcedureHelper.executeUpdate(sql, tx);
			keyEntryMap = new HashMap<String, Constant>();
			keyEntryMap.put("ol_o_id", oidCon);
			keyEntryMap.put("ol_d_id", didCon);
			keyEntryMap.put("ol_w_id", widCon);
			keyEntryMap.put("ol_number", olNumCon);
			orderLineKeys[i][2] = new RecKey("order_line", keyEntryMap);
			insert(orderLineKeys[i][2], sql);
			// record amounts
			totalAmount += olAmount;
		}
		paramHelper.setTotalAmount(
				totalAmount * (1 - paramHelper.getcDiscount()) * (1 + paramHelper.getwTax() + paramHelper.getdTax()));

	}

	@Override
	protected void executeSql() {
		// TODO Auto-generated method stub
		
	}
}
