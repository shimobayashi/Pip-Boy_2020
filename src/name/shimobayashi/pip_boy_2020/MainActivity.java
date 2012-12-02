package name.shimobayashi.pip_boy_2020;

import java.io.IOException;

import org.microbridge.server.AbstractServerListener;
import org.microbridge.server.Server;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
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
}

class MainView extends LinearLayout {
	private Server server;
	private int thermoValue;

	public MainView(Context context, Server server) {
		super(context);

		this.server = server;
		this.server.addListener(new AbstractServerListener() {
			@Override
			public void onReceive(org.microbridge.server.Client client,
					byte[] data) {
				if (data.length < 2)
					return;

				thermoValue = (data[0] & 0xff) | ((data[1] & 0xff) << 8);

				Log.d("thermoValue", ""+thermoValue);
				postInvalidate();
			};
		});
		
		LayoutInflater.from(context).inflate(R.layout.activity_main, this, true);
		setWillNotDraw(false);
	}

	@Override
	public void onDraw(Canvas canvas) {
		TextView textView = (TextView) findViewById(R.id.thermo_label);
		textView.setText("thermo:" + thermoValue);
	}
}