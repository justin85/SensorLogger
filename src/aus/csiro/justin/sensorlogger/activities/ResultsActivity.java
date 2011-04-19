/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aus.csiro.justin.sensorlogger.activities;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import java.util.TimerTask;

import aus.csiro.justin.sensorlogger.R;
import aus.csiro.justin.sensorlogger.SensorLoggerService;
import aus.csiro.justin.sensorlogger.rpc.SensorLoggerBinder;

/**
 *
 * @author chris
 */
public class ResultsActivity extends ListActivity {

	private final Handler handler = new Handler();
	private AutoCompleteTextView input;
	private ProgressDialog dialog;

	private final TimerTask task = new TimerTask() {
		@Override
		public void run() {
			checkStage();
		}
	};

	private ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = SensorLoggerBinder.Stub.asInterface(arg1);
			serviceBound();
		}

		public void onServiceDisconnected(ComponentName arg0) {
			//Toast.makeText(, R.string.error_disconnected, Toast.LENGTH_LONG);
			setResult(RESULT_CANCELED);
			finish();
		}
	};
	SensorLoggerBinder service = null;

	protected void serviceBound() {

		try {
			String name = "activity_" + service.getClassification().substring(11)
			.replace("/", "_").toLowerCase();

			//int res = getResources().getIdentifier(name, "string", "aus.csiro.justin.sensorlogger");
			//((TextView) findViewById(R.id.resultsresultaaa)).setText(res);
		} catch (RemoteException ex) {
			Log.e(getClass().getName(), "Unable to get classification", ex);
		}

		handler.postDelayed(task, 500);
	}

	/** {@inheritDoc} */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// It is not ideal, but we have to set content first because the super.OnCreate
		// calls the service bind initially that then calls serviceBound here and expects
		// the layout.
		//setContentView(R.layout.list_item);
		String[] countries = getResources().getStringArray(R.array.activity_list);

		startService(new Intent(this, SensorLoggerService.class));

		bindService(new Intent(this, SensorLoggerService.class), connection,
				BIND_AUTO_CREATE);

		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, countries));

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				String strCategory = ((TextView) view).getText().toString().toUpperCase();
				try {
					if(strCategory.compareTo("CANCEL") == 0)
					{
						service.setState(1);

						Toast.makeText(getApplicationContext(), "Canceled recording, logger goes to count down",
								Toast.LENGTH_SHORT).show();
						setResult(RESULT_OK);

					}
					else
					{
						Toast.makeText(getApplicationContext(), "Uploading " +((TextView) view).getText().toString().toUpperCase() + " data",
								Toast.LENGTH_LONG).show();
						setResult(RESULT_OK);

						service.submitWithCorrection("CLASSIFIED/"+strCategory);
					}
				} catch (RemoteException e) {
					Log.e(getClass().getName(), "Unable to set state", e);
				}
			}
		});


		//String[] countries = getResources().getStringArray(R.array.activity_list);

		//((Button) findViewById(R.id.resultsyes)).setOnClickListener(yesListener);
		//((Button) findViewById(R.id.resultsno)).setOnClickListener(noListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindService(connection);

	}

	void checkStage() {
		try {
			if (service.getState() == 7) {
				FlurryAgent.onEvent("results_to_thanks");

				service.setState(8);
				startActivity(new Intent(this, ThanksActivity.class));
				finish();
			}
			else if (service.getState() == 1)
			{
				FlurryAgent.onEvent("results_to_countdown");

				service.setState(2);
				startActivity(new Intent(this, CountdownActivity.class));
				finish();

			}
			else {
				handler.postDelayed(task, 100);
			}
		} catch (RemoteException ex) {
			Log.e(getClass().getName(), "Unable to get state", ex);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onStart() {
		super.onStart();

		FlurryAgent.onStartSession(this, "TFBJJPQUQX3S1Q6IUHA6");
	}

	/** {@inheritDoc} */
	@Override
	protected void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(this);
	}

}
