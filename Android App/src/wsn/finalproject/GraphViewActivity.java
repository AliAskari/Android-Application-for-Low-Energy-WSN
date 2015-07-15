package wsn.finalproject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

public class GraphViewActivity extends Activity {

	private BaseApplication appHelper;
	private DbxAccountManager mAccountManager;
	private DbxDatastore mStore;
	private DbxTable mTable;

	private String mSessionName;
	private ListView l;
	private int[] mNodeID;
	private double[] mBTData;
	private int mNodeNumber;
	private String mTableName;

	private GraphViewData[] data;
	private int mNumberOfNodes;
	private List<GraphViewSeries> mNoideSeries;
	private List<Integer> mGraphColor;
	private GraphViewSeries mTempGraph;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGraphColor = new ArrayList<Integer>();

		mGraphColor.add(Color.MAGENTA);
		mGraphColor.add(Color.RED);
		mGraphColor.add(Color.GREEN);
		mGraphColor.add(Color.YELLOW);
		mGraphColor.add(Color.WHITE);

		Bundle bundle = getIntent().getExtras();
		mTableName = bundle.getString("Table_Name");
		mNumberOfNodes = 5;

		appHelper = ((BaseApplication) getApplicationContext());
		mAccountManager = appHelper.getAccountManager();
		mSessionName = appHelper.getSessionName();
		//mNumberOfNodes = appHelper.getNodeNumber();

		mStore = appHelper.getStore();
		mNoideSeries = new ArrayList<GraphViewSeries>();
	}

	protected void onResume() {
		super.onResume();
		Log.i("Final Project", "inside On Resume");
		createTable();
		Log.i("Final Project", "After create table");
		createGraph();
	}

	public void createTable() {
		try {
			Log.i("Final Project", "inside create table - before getTable");
			mTable = mStore.getTable(mTableName);
			Log.i("Final Project", "inside On Resume - after getTable");
			mStore.sync();
		} catch (DbxException e) {
			handleException(e);
		}
	}

	public void createGraph() {
		LineGraphView graphView = new LineGraphView(this, mTableName
				+ " Session");

		graphView.getGraphViewStyle().setTextSize(12);
		graphView.setViewPort(0, 10);
		graphView.setShowLegend(true);
		graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
		graphView.getGraphViewStyle().setVerticalLabelsColor(Color.WHITE);
		graphView.setScrollable(true);
		graphView.setScalable(true);
		graphView.setManualYAxis(true);
		graphView.setManualYAxisBounds(3.3, 0);

		Log.i("Final Project",
				"after query params " + String.valueOf(mNumberOfNodes));
		for (int j = 1; j <= mNumberOfNodes; j++) {
			Log.i("Final Project", "inside J loop");
			try {
				DbxFields queryParams = new DbxFields().set("ID", j);
				Log.i("Final Project", "node ID number " + String.valueOf(j));
				DbxTable.QueryResult results = null;
				results = mTable.query(queryParams);

				Log.i("Final Project", "after query params");

				Iterator<DbxRecord> res = results.iterator();

				int num = results.count();
				Log.i("Final Project",
						"after result count" + String.valueOf(num));

				if (num != 0) {
					data = new GraphViewData[num];

					for (int i = 0; i < num; i++) {
						data[i] = new GraphViewData(i, res.next().getDouble(
								"Data"));
					}
				}
			} catch (DbxException e) {
				e.printStackTrace();
			}
			mNoideSeries.add(new GraphViewSeries("Node[" + j + "]",
					new GraphViewSeriesStyle(mGraphColor.get(j - 1), 2), data));

			Log.i("Final Project", " After mnodeseries");

		}

		for (int j = 0; j < mNumberOfNodes; j++) {
			graphView.addSeries(mNoideSeries.get(j));
		}

		RelativeLayout relativeLayout = new RelativeLayout(this);
		relativeLayout.setBackgroundColor(Color.BLACK);

		relativeLayout.addView(graphView);
		setContentView(relativeLayout);
	}

	private void handleException(DbxException e) {
		e.printStackTrace();
		Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStop() {
		super.onStop();
		appHelper = null;
		mAccountManager = null;
		mStore = null;
		mTable = null;

		mSessionName = null;

		mNodeID = null;
		mBTData = null;
		mNodeNumber = 0;
		mTableName = null;

		data = null;
		mNumberOfNodes = 0;
		mNoideSeries = null;
		mGraphColor = null;
		mTempGraph = null;

	}
}
