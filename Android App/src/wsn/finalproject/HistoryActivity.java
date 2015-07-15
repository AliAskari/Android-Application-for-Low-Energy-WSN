package wsn.finalproject;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

public class HistoryActivity extends Activity implements OnItemClickListener {

	private ArrayList<String> availableTables;
	private ArrayAdapter<String> adapter;

	private BaseApplication appHelper;
	private DbxAccountManager mAccountManager;
	private DbxDatastore mStore;
	private DbxTable mTable;

	private String mSessionName;
	private ListView l;
	private int mNodeNumber;
	private String temp;

	private int mNumberOfNodes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history);
		appHelper = ((BaseApplication) getApplicationContext());		

		availableTables = new ArrayList<String>();
		l = (ListView) findViewById(R.id.listView1);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, availableTables);
		l.setAdapter(adapter);
		l.setOnItemClickListener(this);
	}

	protected void onResume() {
		super.onResume();
		availableTables.clear();
		try {
			appHelper = ((BaseApplication) getApplicationContext());
			mStore = appHelper.getStore();
			queryTables();
		} catch (DbxException e) {
			handleException(e);
		}
		adapter.notifyDataSetChanged();
	}


	public void queryTables() throws DbxException {
		Iterator<DbxTable> mTables = mStore.getTables().iterator();
		while (mTables.hasNext()) {
			availableTables.add(mTables.next().getId().toString());
		}
		adapter.notifyDataSetChanged();
	}

	public void deleteTable() throws DbxException {
		DbxTable.QueryResult results = mTable.query();
		Iterator<DbxRecord> res = results.iterator();
		while (res.hasNext()) {
			res.next().deleteRecord();
		}
		mStore.sync();
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		TextView tempr = (TextView) view;
		mTable = mStore.getTable(tempr.getText().toString());
		Intent historyIt = new Intent(this, GraphViewActivity.class);
		historyIt.putExtra("Table_Name", mTable.getId().toString());
		startActivity(historyIt);		
	}
	
	
	
	private void handleException(DbxException e) {
		e.printStackTrace();
		Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
	}
}
