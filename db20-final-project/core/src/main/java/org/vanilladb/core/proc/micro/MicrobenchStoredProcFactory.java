package org.vanilladb.core.proc.micro;

//import org.vanilladb.bench.benchmarks.micro.MicrobenchTransactionType;
import org.vanilladb.core.sql.storedprocedure.StoredProcedure;
import org.vanilladb.core.sql.storedprocedure.StoredProcedureFactory;

public class MicrobenchStoredProcFactory implements StoredProcedureFactory {

	@Override
	public StoredProcedure<?> getStroredProcedure(int pid) {
		StoredProcedure<?> sp;
		switch (MicrobenchTransactionType.fromProcedureId(pid)) {
		case TESTBED_LOADER:
			sp = new MicroTestbedLoaderProc();
			break;
		case CHECK_DATABASE:
			sp = new MicroCheckDatabaseProc();
			break;
		case MICRO_TXN:
			sp = new MicroTxnProc();
			break;
		default:
			throw new UnsupportedOperationException("The benchmarker does not recognize procedure " + pid + "");
		}
		return sp;
	}
}
