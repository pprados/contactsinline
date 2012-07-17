package fr.prados.contacts.ui;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import fr.prados.contacts.lib.R;

public final class HelpActivity extends Activity
{
	private static final String	PREFERENCES_INTRO			= "intro";
	private static final String	PREFERENCES_SHOWED			= "showed";

	/**
	 * Displays the introduction. This method should be called from the onCreate() method of
	 * your main Activity.
	 * 
	 * @param activity
	 *            The Activity to finish if the user rejects the EULA.
	 */
	public static void showIntro(final Activity activity)
	{
		final SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES_INTRO,Activity.MODE_PRIVATE);
		if (!preferences.getBoolean(PREFERENCES_SHOWED, false))
		{
			preferences.edit().putBoolean(PREFERENCES_SHOWED, true).commit();
			activity.startActivity(new Intent(activity,HelpActivity.class));
		}
	}

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		setTitle(R.string.help_title);
		findViewById(R.id.header).setVisibility(View.GONE);
		View v=findViewById(R.id.call_view);
		if (v!=null)
		{
			v.setVisibility(View.GONE);
			v.setEnabled(false);
		}
		((TextView)findViewById(R.id.name)).setText(R.string.help_sample_name);
		findViewById(R.id.contacts).setEnabled(false);
		QuickContactBadge photo=(QuickContactBadge)findViewById(R.id.photo);
		photo.setEnabled(false);
		// Cache miss
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
		{
			photo.setImageToDefault();
		}
		else
		{
			photo.setImageResource(R.drawable.ic_contact_list_picture);
		}
	}
	public void onOk(View view)
	{
		finish();
	}
	@Override
	protected void onResume()
	{
		super.onResume();
		hideKeyboard();
	}
	private void hideKeyboard()
	{
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
				findViewById(R.id.help_part1).getWindowToken(), 0);
	}
	
}
