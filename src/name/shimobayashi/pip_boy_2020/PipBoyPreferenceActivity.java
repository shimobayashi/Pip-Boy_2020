package name.shimobayashi.pip_boy_2020;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PipBoyPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
