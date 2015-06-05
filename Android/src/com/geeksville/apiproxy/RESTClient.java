package com.geeksville.apiproxy;

import java.io.File;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.*;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.entity.FileEntity;
import com.geeksville.apiproxy.APIConstants;
import org.json.*;
import org.apache.http.client.utils.URLEncodedUtils;
import java.util.LinkedList;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import java.io.IOException;

public class RESTClient {

	private static DefaultHttpClient getHttpClient() {
		// new DefaultHttpClient()
		// use following code to solve Adapter is detached error
		// refer:
		// http://stackoverflow.com/questions/5317882/android-handling-back-button-during-asynctask
		BasicHttpParams params = new BasicHttpParams();

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));

		// Set the timeout in milliseconds until a connection is established.
		// HttpConnectionParams.setConnectionTimeout(params,
		// CONNECTION_TIMEOUT);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		// HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);

		return new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				schemeRegistry), params);
	}

	static private DefaultHttpClient httpclient = getHttpClient();

	/**
	 * @return on success the URL to view the flight at, or NULL if the flight
	 *         should never be uploaded again
	 */
	public static String doUpload(File srcFile, String userId, String userPass,
			String vehicleId, String apiKey, String privacy) throws IOException {
		String baseUrl = APIConstants.URL_BASE;
		LinkedList<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("api_key", apiKey));
		params.add(new BasicNameValuePair("login", userId));
		params.add(new BasicNameValuePair("password", userPass));
		params.add(new BasicNameValuePair("privacy", privacy));
		params.add(new BasicNameValuePair("autoCreate", "true"));
		String queryParams = URLEncodedUtils.format(params, "utf-8");
		String webAppUploadUrl = String.format(
				"%s/api/v1/mission/upload/%s?%s", baseUrl, vehicleId,
				queryParams);

		try {
			// instantiates httpclient to make request

			// url with the post data
			System.out.println("Starting upload to " + baseUrl);
			HttpPost httpost = new HttpPost(webAppUploadUrl);

			FileEntity se = new FileEntity(srcFile, APIConstants.TLOG_MIME_TYPE);
			httpost.setEntity(se);

			// sets a request header so the page receving the request
			// will know what to do with it
			httpost.setHeader("Accept", "application/json");

			// Handles what is returned from the page
			BasicResponseHandler responseHandler = new BasicResponseHandler();
			String resp = httpclient.execute(httpost, responseHandler);

			System.out.println("Received JSON response: " + resp);

			JSONArray missions = new JSONArray(resp);
			if (missions.length() != 1)
				throw new IOException("The server rejected this log file");

			JSONObject mission = missions.getJSONObject(0);
			String viewURL = mission.getString("viewURL");

			System.out.println("View URL is " + viewURL);

			return viewURL;
		} catch (HttpResponseException ex) {
			if (ex.getStatusCode() == HttpStatus.SC_NOT_ACCEPTABLE)
				return null;
			else
				throw ex;
		} catch (JSONException ex) {
			throw new IOException("Malformed server response: "
					+ ex.getMessage());
		}
	}

}
