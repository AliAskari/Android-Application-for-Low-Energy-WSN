package wsn.finalproject;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Application;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

public class BaseApplication extends Application {

	private static final String APP_KEY = "************"; // put your app key here, dont foget to update manifest!
	private static final String APP_SECRET = "**********"; // put your app secret here
	private DbxAccountManager mAccountManager;
	private DbxDatastore mStore;
	private DbxTable mCurrentTable;
	private DbxTable.QueryResult results;

	private String mSessionName;
	private ArrayList<String> availableTables;
	private int mNodeNumber;

	@Override
	public void onCreate() {
		super.onCreate();
		mAccountManager = DbxAccountManager.getInstance(
				getApplicationContext(), APP_KEY, APP_SECRET);
		mNodeNumber = 0;
	}

	public DbxAccountManager getAccountManager() {
		return mAccountManager;
	}

	public DbxDatastore getDataStore() {
		createDataStore();
		return mStore;
	}

	public DbxDatastore getStore() {
		return mStore;
	}

	public DbxTable getTableee() {
		return mCurrentTable;
	}

	public DbxTable getCreateTable() {
		createTable();
		return mCurrentTable;
	}

	public void setSessionName(String mSessionName) {
		this.mSessionName = mSessionName;
	}

	public String getSessionName() {
		return this.mSessionName;
	}

	public ArrayList<String> getTables() throws DbxException {
		Iterator<DbxTable> mTables = mStore.getTables().iterator();
		while (mTables.hasNext()) {
			availableTables.add(mTables.next().getId().toString());
		}
		return availableTables;
	}

	private void createDataStore() {
		try {
			if (null == mStore) {
				mStore = DbxDatastore.openDefault(mAccountManager
						.getLinkedAccount());
			}
		} catch (DbxException e) {
			handleException(e);
		}
	}

	private void createTable() {
		try {
			mCurrentTable = mStore.getTable(mSessionName);
			mStore.sync();
		} catch (DbxException e) {
			handleException(e);
		}
	}

	public void deleteTable() throws DbxException {
		results = mCurrentTable.query();
		Iterator<DbxRecord> res = results.iterator();
		while (res.hasNext()) {
			res.next().deleteRecord();
		}
		mStore.sync();
	}
	
	public void setNodeNumber(int i){
		this.mNodeNumber  = i;
	}

	public int getNodeNumber(){
		return this.mNodeNumber;
	}
	
	public void closeDataStore() {
		if (null != mStore) {
			mStore.close();
			mStore = null;
			mCurrentTable = null;
			results = null;
			mSessionName = null;
			availableTables = null;
		}
	}

	public void handleException(DbxException e) {
		e.printStackTrace();
		Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
	}
}
