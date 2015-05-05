
package net.qb9.hades;

import android.content.Context;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.content.SharedPreferences;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import org.json.JSONObject;
import java.io.InputStreamReader;
import org.json.JSONTokener;
import org.json.JSONException;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class GCMHelper {

	static final String TAG = "GCMHelper";
	private static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";

	private Context context;
	private GoogleCloudMessaging gcm;
	private String regid;
	private SharedPreferences prefs;
	private AsyncTask task;
	private String userid;

	public GCMHelper(Context i_context, SharedPreferences i_prefs, String i_userid) {
		context = i_context;
		prefs = i_prefs;
		userid = i_userid;
	}

	public void init() {
		gcm = GoogleCloudMessaging.getInstance(context);
		regid = getRegistrationId(context);
		if ( regid.isEmpty() ) {
			registerInBackground();
		}
	}

	private String getRegistrationId(Context context) {
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing registration ID is not guaranteed to work with
		// the new app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	private void registerInBackground() {
		class RegisterTask extends AsyncTask<Object, Void, Integer> {
			@Override
			protected Integer doInBackground(Object... params) {
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					regid = gcm.register(HadesConfig.NOTIF_SENDER_ID);
					Log.i(TAG, "Device registered, registration ID=" + regid);
					if ( sendRegistrationIdToBackend() ) {
						storeRegistrationId(context, regid);
					}
				} catch (IOException ex) {
					Log.e(TAG, "Error :" + ex.getMessage());
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
				}
				return 0;
			}
		}
		task = new RegisterTask();
		task.execute(null, null, null);
	}

	private final static char[] hexArray = "0123456789abcdef".toCharArray();

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private static String hmacSha1(String value, String key)
			throws UnsupportedEncodingException, NoSuchAlgorithmException,
			InvalidKeyException {
		String type = "HmacSHA1";
		SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
		Mac mac = Mac.getInstance(type);
		mac.init(secret);
		byte[] bytes = mac.doFinal(value.getBytes());
		return bytesToHex(bytes);
	}

	private boolean sendRegistrationIdToBackend() {
		HttpURLConnection urlConnection = null;
		try {

			JSONObject data = new JSONObject();
			data.put("gameid", HadesConfig.GAMEID);
			data.put("userid", userid);
			data.put("device_token", regid);
			data.put("ostype", "android");
			String dataStr = data.toString();

			URL url = new URL(HadesConfig.NOTIF_REGISTER_URL);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.addRequestProperty("Content-Type", "application/json");
			urlConnection.addRequestProperty("X-Security", "hmac-sha1 " + hmacSha1(data.toString(), HadesConfig.CLIENT_KEY));
			OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
			out.write(dataStr.getBytes());
			out.flush();
			int responseCode = urlConnection.getResponseCode();
			if ( responseCode != 200 ) {
				throw new Exception("bad response code " + responseCode);
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
			    response.append(line);
			}

			JSONObject json = (JSONObject) new JSONTokener(response.toString()).nextValue();
			if ( json == null ) {
				throw new Exception("Error parsing JSON response!");
			}
			String result = json.getString("result");
			if ( ! result.equals("OK") ) {
				throw new Exception("Error registering device! (" + result + ")");
			}

			Log.i(TAG, "OK registering device (" + result + ")");

		} catch (Exception e) {
			Log.e(TAG, "Error sending registration: " + e);
			return false;
		} finally {
			if ( urlConnection != null ) urlConnection.disconnect();
		}
		return true;
	}

	private void storeRegistrationId(Context context, String regId) {
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

}
