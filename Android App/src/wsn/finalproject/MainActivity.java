package wsn.finalproject;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxTable;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

//==============================================================================
// Variables:
public class MainActivity extends Activity {
	public static final int NumberOfSamplesPerUpload = 1000;
	public int SampleCounter = 0;
	public static final byte NewLineChar = 13;
	private BluetoothAdapter mBluetoothAdapter = null;
	private boolean mEnablingBT;
	public int RecData = 0;
	public Button connectbtn;
	public Button disconnectbtn;
	public String toastText = "";
	public String recData = "";
	public byte[] DataBuffer = new byte[1024];
	BluetoothSocket mmSocket;
	OutputStream mmOutputStream;
	int dataCount = 1;
	public boolean ConnectGreenIconVIS = true; // Connect circle button
												// visibility
	private static BluetoothSerialService mSerialService = null;
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Activity Result Keys
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int CONNECTION_LOST = 6;
	public static final int UNABLE_TO_CONNECT = 7;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	public static final String InCommingData = " ";

	private MenuItem mMenuItemConnect;
	// Name of the connected device
	private String mConnectedDeviceName = null;

	// Storage File
	File folder, DataFile, FileToBeSent;
	String DataFileName = "DataFile.txt";

	// Network availability
	private boolean NetworkAvalability = false;

	// Dropbox variables
	private BaseApplication appHelper;
	private static final int REQUEST_LINK_TO_DBX = 0;
	private DbxAccountManager mAccountManager;
	private DbxDatastore mStore;
	private DbxTable mCurrentTable;
	private DbxFields tableFields;
	private String mSessionNameText;

	private TextView mSessionNameView;
	private int mNodeID;
	private int mNodeData;
	private boolean mDecide;
	private SparseIntArray mIdData;
	private int mNumberOfNodes;
	private int mNodeNumber;
	private List<Integer> mMaxNode;
	private TextView nodeText;
	private int mBlueToothData;
	
	
	///// GRAPHVIEW VARIABLES
    private GraphViewSeries mNode1,mNode2,mNode3, mNode4, mNode5;
    private GraphView graphView;
    public static int GraphBufferSize = 100;
	private List<GraphViewData> Xdata1, Xdata2, Xdata3, Xdata4, Xdata5;
	private static float graph2LastXValue=0;
	

	// ==============================================================================
	// Android CallBacks:
	// ==============================================================================
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mSerialService = new BluetoothSerialService(this, mHandlerBT);
		if (mBluetoothAdapter == null) {
			finishDialogNoBluetooth();
			return;
		}
		if (!isNetworkAvailable()) {
			Toast.makeText(
					this,
					"No Internet Connection Found! \nPlease check your connectivity.",
					Toast.LENGTH_LONG).show();
			NetworkAvalability = false;
		} else {
			Toast.makeText(this, "Internet Connection Found!",
					Toast.LENGTH_LONG).show();
			NetworkAvalability = true;
		}
		mAccountManager = null;
		appHelper = ((BaseApplication) getApplicationContext());
		mAccountManager = appHelper.getAccountManager();
		mSessionNameText = null;
		mStore = null;
		mSessionNameView = (TextView) findViewById(R.id.test_text);
		nodeText = (TextView) findViewById(R.id.node_number);
		mIdData = new SparseIntArray();
		mDecide = true;
		mNodeID = 0;
		mNodeData = 0;
		mNumberOfNodes = 0;
		mNodeNumber = 2;
		mMaxNode = new ArrayList<Integer>();
		mBlueToothData = 0;
	}

	// ==============================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		mMenuItemConnect = menu.getItem(0);
		return true;
	}

	// ==============================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.connect:

			if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
				// Launch the DeviceListActivity to see devices and do scan
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			} else if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
				mSerialService.stop();
				// mSerialService.start();
			}
			return true;

		case R.id.link_dropbox:
			if (!mAccountManager.hasLinkedAccount()) {
				mAccountManager.startLink((Activity) MainActivity.this,
						REQUEST_LINK_TO_DBX);
			} else {
				Toast.makeText(this,
						"The app is already linked to your DropBox Account!",
						Toast.LENGTH_SHORT).show();
			}
			startLogging();
		}
		/*
		 * mSessionNameText = String.valueOf(new Date());
		 * mSessionNameView.setText(mSessionNameText);
		 */
		return false;
	}

	// ==============================================================================
	@Override
	public void onStart() {
		super.onStart();
		mEnablingBT = false;
		InitGraphView();
	}

	// ==============================================================================
	@Override
	protected synchronized void onResume() {
		super.onResume();
		if (!mEnablingBT) {
			if ((mBluetoothAdapter != null) && (!mBluetoothAdapter.isEnabled())) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.alert_dialog_turn_on_bt)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(R.string.alert_dialog_warning_title)
						.setCancelable(false)
						.setPositiveButton(R.string.alert_dialog_yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										mEnablingBT = true;
										Intent enableIntent = new Intent(
												BluetoothAdapter.ACTION_REQUEST_ENABLE);
										startActivityForResult(enableIntent,
												REQUEST_ENABLE_BT);
									}
								})
						.setNegativeButton(R.string.alert_dialog_no,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										finishDialogNoBluetooth();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
			if (mSerialService != null) {
				if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
					mSerialService.start();
				}
			}
		}
	}

	// ==============================================================================
	@Override
	protected void onRestart() {
		super.onRestart();
		setContentView(R.layout.activity_main);
		mSessionNameView = (TextView) findViewById(R.id.test_text);
		nodeText = (TextView) findViewById(R.id.node_number);
		nodeText.setText("Number of available nodes: " + String.valueOf(appHelper.getNodeNumber()));
		if(mBlueToothData != 0){
			mSessionNameView.setText(String.valueOf(mBlueToothData));
		}
	}
	// ==============================================================================
	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage("Do you really want to exit?")
				.setCancelable(false)
				.setPositiveButton("Quit",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								mSerialService.stop();
								MainActivity.this.finish();
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// ==============================================================================
	// On Activity Result:
	// ==============================================================================
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:

			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mSerialService.connect(device);
			}
			break;
		case REQUEST_LINK_TO_DBX:
			if (requestCode == REQUEST_LINK_TO_DBX) {
				if (resultCode == Activity.RESULT_OK) {
					Toast.makeText(this, "Succefully linked to DropBox!",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this,
							"Link to Dropbox failed or was cancelled.",
							Toast.LENGTH_SHORT).show();
				}
			} else {
				super.onActivityResult(requestCode, resultCode, data);
			}
		}
	}

	// ==============================================================================
	// Handles:
	// ==============================================================================
	private final Handler mHandlerBT = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothSerialService.STATE_CONNECTED:
					break;

				case BluetoothSerialService.STATE_CONNECTING:
					Toast.makeText(getApplicationContext(),
							"Connecting.. " + mConnectedDeviceName,
							Toast.LENGTH_SHORT).show();
					break;

				case BluetoothSerialService.STATE_LISTEN:
					break;
				case BluetoothSerialService.STATE_NONE:
					break;
				}
				break;
			/*
			 * case MESSAGE_WRITE: if (mLocalEcho) { byte[] writeBuf = (byte[])
			 * msg.obj; mEmulatorView.write(writeBuf, msg.arg1); }
			 * 
			 * break;
			 */

			case MESSAGE_READ:
				UpdateData(msg.arg1);
				// ParseIncomData(msg.arg1);
				// Log.e("ReadMsg", "Received Data: "+msg.arg1);
				break;

			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				if (mMenuItemConnect != null) {
					mMenuItemConnect.setTitle(R.string.disconnect);
				}
				break;

			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;

			case CONNECTION_LOST:

				Toast.makeText(getApplicationContext(),
						"Device connection was lost", Toast.LENGTH_SHORT)
						.show();
				if (mMenuItemConnect != null) {
					mMenuItemConnect.setTitle(R.string.connect);
				}

				break;

			case UNABLE_TO_CONNECT:
				ConnectGreenIconVIS = false;
				Toast.makeText(getApplicationContext(),
						"Unable to connect device", Toast.LENGTH_SHORT).show();
				break;

			}
		}
	};

	// ==============================================================================
	// User Methods:
	// ==============================================================================
	public void finishDialogNoBluetooth() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.alert_dialog_no_bt)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.app_name)
				.setCancelable(false)
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								finish();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// ==============================================================================
	public int getConnectionState() {
		return mSerialService.getState();
	}

	// ==============================================================================
	public void UpdateData(int Val) {
		mBlueToothData = Val;
		mSessionNameView.setText(String.valueOf(mBlueToothData));
		if (null != mStore) {
			Log.i("Final Project", "mstore is not Null");
			if (Val != 0) {
				if (mDecide == true) {
					mDecide = false;
					mNodeID = Val;
				} else {
					mDecide = true;
					mNodeData = Val;
				}
				mNumberOfNodes = mNumberOfNodes + 1;
				mIdData.put(mNodeID, mNodeData);
			} else if (Val == 0) {
				try {
					mDecide = true;
					addRows();
					UpdateGraph(mIdData);
					mStore.sync();
				} catch (DbxException e) {
					appHelper.handleException(e);
				}
			}
		}
		
	}

	private void startLogging() {
		if (mAccountManager.hasLinkedAccount()) {
			mSessionNameText = "Demo5";
			appHelper.setSessionName(mSessionNameText);
			mStore = appHelper.getDataStore();
			mCurrentTable = appHelper.getCreateTable();
			Toast.makeText(this, mSessionNameText + " table is created",
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, mSessionNameText + "No linked Account found",
					Toast.LENGTH_SHORT).show();
		}

	}

	public void addRows() {		
		Log.i("Final Project",
				"addRows is called" + String.valueOf((mNumberOfNodes / 2)));
		int key = 0;
		for (int i = 0; i < (mNumberOfNodes / 2); i++) {
			Log.i("Final Project", "Inside the addRow loop");
			key = mIdData.keyAt(i);
			int value = mIdData.get(key);
			tableFields = new DbxFields().set("ID", key).set("Data",
					ConvertToVoltage(value));
			mCurrentTable.insert(tableFields);
		}
		appHelper.setNodeNumber(mNumberOfNodes/2);
		nodeText.setText("");
		nodeText.setText("Number of available nodes: " + String.valueOf(appHelper.getNodeNumber()) );
		mNumberOfNodes = 0;
	}

	public float ConvertToVoltage(int val) {
		float ConvertedVal;
		ConvertedVal = (float) ((((float) val) / 65536) * 3.3);
		return ConvertedVal;

	}

	public void openHistory(View v) {
		if ((mStore != null) & mAccountManager.hasLinkedAccount()) {
			Intent intent = new Intent(this, HistoryActivity.class);
			startActivity(intent);
		}else{
			Toast.makeText(MainActivity.this, "Not linked to DropBox!", Toast.LENGTH_SHORT).show();
		}
	}

	public void UpdateGraph(SparseIntArray Val){
		graph2LastXValue += 1d;
		mNode1.appendData(new GraphViewData(graph2LastXValue, ConvertToVoltage(mIdData.get(0))), true, 10);
		mNode2.appendData(new GraphViewData(graph2LastXValue, ConvertToVoltage(mIdData.get(1))), true, 10);
		mNode3.appendData(new GraphViewData(graph2LastXValue, ConvertToVoltage(mIdData.get(2))), true, 10);
		mNode4.appendData(new GraphViewData(graph2LastXValue, ConvertToVoltage(mIdData.get(3))), true, 10);
		mNode5.appendData(new GraphViewData(graph2LastXValue, ConvertToVoltage(mIdData.get(4))), true, 10);		
		dataCount = dataCount + 1;
		}
	
	public void InitGraphView() {

	    mNode1 = new GraphViewSeries("Node1 Voltage",
	    		new GraphViewSeriesStyle(Color.BLUE, 2),
	    		new GraphViewData[]{new GraphViewData(0,0)});
	    
	    mNode2 = new GraphViewSeries("Node2 Voltage",
	    		new GraphViewSeriesStyle(Color.GREEN, 2),
	    		new GraphViewData[]{new GraphViewData(0,0)});
	    
	    mNode3 = new GraphViewSeries("Node3 Voltage",
	    		new GraphViewSeriesStyle(Color.RED, 2),
	    		new GraphViewData[]{new GraphViewData(0,0)});
	    
	    mNode4 = new GraphViewSeries("Node4 Voltage",
	    		new GraphViewSeriesStyle(Color.CYAN, 2),
	    		new GraphViewData[]{new GraphViewData(0,0)});
	    
	    mNode5 = new GraphViewSeries("Node5 Voltage",
	    		new GraphViewSeriesStyle(Color.YELLOW, 2),
	    		new GraphViewData[]{new GraphViewData(0,0)});
	    
	    
	    graphView = new LineGraphView(this,"Node1 Data");
	    graphView.getGraphViewStyle().setTextSize(12);
	    graphView.setViewPort(0, 10);
	    graphView.addSeries(mNode1);
	    graphView.addSeries(mNode2);
	    graphView.addSeries(mNode3);
	    graphView.addSeries(mNode4);
	    graphView.addSeries(mNode5); // data

	    graphView.setShowLegend(true);
	    graphView.setLegendAlign(LegendAlign.BOTTOM);
	    graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
	    graphView.getGraphViewStyle().setVerticalLabelsColor(Color.WHITE);
	    graphView.setScrollable(true);
	    graphView.setScalable(true);
	    graphView.setManualYAxis(true);
	    graphView.setManualYAxisBounds(3.3, 0);
	    LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
	    layout.addView(graphView);   
	    }
	
	// ==============================================================================
	public void message(String message) {
		Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
	}

	// ==============================================================================
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		appHelper.closeDataStore();
		if (mSerialService != null)
			mSerialService.stop();
	}
}
///Salam ali