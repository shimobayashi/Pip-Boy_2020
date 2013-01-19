package name.shimobayashi.pip_boy_2020;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.microbridge.server.AbstractServerListener;
import org.microbridge.server.Server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final int REQUEST_CODE = 200; // For call preference activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set full screen view, no title
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Create TCP server
		Server server = null;
		try {
			server = new Server(4567);
			server.start();
		} catch (IOException e) {
			Log.e("microbridge", "Unable to start TCP server", e);
			System.exit(-1);
		}

		MainView view = new MainView(this, server);
		setContentView(view);
		view.requestFocus();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, PipBoyPreferenceActivity.class);
			startActivityForResult(intent, REQUEST_CODE);
			return true;
		}
		return false;
	}
}

class MainView extends LinearLayout {
	private Server server;

	// IN
	private double temperature; // C
	private double humidity; // %
	private int pressure; // Pa
	private int dataLength;
	private int responseCode = 0;

	// OUT
	private int motor;
	private int led;

	public MainView(Context context, Server server) {
		super(context);

		this.server = server;
		this.server.addListener(new AbstractServerListener() {
			@Override
			public void onReceive(org.microbridge.server.Client client,
					byte[] data) {
				dataLength = data.length;
				
				if (data.length < 8)
					return;

				int thermoValue = (data[0] & 0xff) | ((data[1] & 0xff) << 8);
				int humidityValue = (data[2] & 0xff) | ((data[3] & 0xff) << 8);
				long pressureValue = (data[4] & 0xff) | ((data[5] & 0xff) << 8)
						| ((data[6] & 0xff) << 16) | ((data[7] & 0xff) << 24);
				update(thermoValue, humidityValue, pressureValue);
			};
		});

		LayoutInflater.from(context)
				.inflate(R.layout.activity_main, this, true);
		setWillNotDraw(false); // Need for call onDraw via invalidate

		SeekBar motorSeekbar = (SeekBar) findViewById(R.id.motor_seekbar);
		motorSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				motor = progress;
				send();
			}
		});

		SeekBar ledSeekbar = (SeekBar) findViewById(R.id.led_seekbar);
		ledSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				led = progress;
				send();
			}
		});
	}

	private void send() {
		try {
			server.send(new byte[] { (byte) motor, (byte) led });
		} catch (IOException e) {
			Log.e("microbridge", "problem sending TCP message", e);
		}
	}

	private void update(int thermoValue, int humidityValue, long pressureValue) {
		// Calc values
		temperature = (thermoValue * (5.0 / 1024)) * 100; // Voltage * 100C
		humidity = (humidityValue * (5.0 / 1024)) * 100; // Voltage * 100%
		pressure = (int)pressureValue;

		// PUT to COSM
		responseCode = -1;
		Context context = getContext();
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		int feedId = sp.getInt("cosm_feed_id", 89487);
		URI url = URI.create("http://api.cosm.com/v2/feeds/" + feedId);
		HttpPut request = new HttpPut(url);
		String apiKey = sp.getString("cosm_api_key", "");
		request.addHeader("X-ApiKey", apiKey);
		StringEntity entity;
		try {
			entity = new StringEntity(
					"{\"datastreams\":[{\"id\":\"temperature\",\"current_value\":\""
							+ temperature
							+ "\"},{\"id\":\"humidity\",\"current_value\":\""
							+ humidity
							+ "\"},{\"id\":\"pressure\",\"current_value\":\""
							+ pressure + "\"}]}", "UTF-8");
			entity.setContentType("application/x-www-form-urlencoded");
			request.setEntity(entity);

			// Do PUT
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			responseCode = response.getStatusLine().getStatusCode();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Draw
		postInvalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		TextView textView;

		// Temperature
		textView = (TextView) findViewById(R.id.thermo_label);
		textView.setText("temperature:" + temperature);

		// Humidity
		textView = (TextView) findViewById(R.id.humidity_label);
		textView.setText("humidity:" + humidity);

		// Pressure
		textView = (TextView) findViewById(R.id.pressure_label);
		textView.setText("pressure:" + pressure);

		// Data Length
		textView = (TextView) findViewById(R.id.data_label);
		textView.setText("data:" + dataLength);

		// Data Length
		textView = (TextView) findViewById(R.id.status_label);
		textView.setText("status:" + responseCode);

		// Datetime
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		textView = (TextView) findViewById(R.id.datetime_label);
		textView.setText(sdf.format(date));
	}
}