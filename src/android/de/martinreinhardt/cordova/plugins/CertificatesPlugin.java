package de.martinreinhardt.cordova.plugins;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.json.JSONArray;
import org.json.JSONException;

public class CertificatesPlugin extends CordovaPlugin {

  /**
   * Logging Tag
   */
  private static final String LOG_TAG = "Certificates";

  /**
   * Untrusted Variable
   */
  private boolean allowUntrusted = false;

  private String trustedThumbprint = null;

  private CertificatesCordovaWebViewClient cWebClient;

  /**
   * Executes the request.
   *
   * This method is called from the WebView thread. To do a non-trivial amount
   * of work, use: cordova.getThreadPool().execute(runnable);
   *
   * To run on the UI thread, use:
   * cordova.getActivity().runOnUiThread(runnable);
   *
   * @param action
   *                        The action to execute. (Currently "setUntrusted only")
   * @param args
   *                        The exec() arguments.
   * @param callbackContext
   *                        The callback context used when calling back into
   *                        JavaScript.
   * @return Whether the action was valid.
   *
   *
   */
  @Override
  public boolean execute(String action, JSONArray args,
      CallbackContext callbackContext) throws JSONException {
    Boolean success = false;
    if (action.equals("setUntrusted")) {
      allowUntrusted = args.getBoolean(0);
      Log.d(LOG_TAG, "Setting allowUntrusted to " + allowUntrusted);

      success = true;
    } else if (action.equals("setTrustedThumbprint")) {
      this.trustedThumbprint = args.getString(0);
      Log.d(LOG_TAG, "Setting trusted thumbprint to " + this.trustedThumbprint);
      success = true;
    } else if (action.equals("getLatestRequestCertificateThumbprint")) {
      callbackContext.success(this.cWebClient.getLatestRequestThumbprint());
      return true;
    }

    try {
      cordova.getActivity().runOnUiThread(new Runnable() {
        public void run() {
          try {
            CordovaActivity ca = (CordovaActivity) cordova.getActivity();
            SystemWebView view = (SystemWebView) webView.getView();
            cWebClient = new CertificatesCordovaWebViewClient((SystemWebViewEngine) webView.getEngine());

            cWebClient.setAllowUntrusted(allowUntrusted);
            cWebClient.setTrustedThumbprint(trustedThumbprint);
            webView.clearCache();

            // this fixes a crash that displays the error message:
            // "The connection to the server was unsuccessful.
            webView.stopLoading();
            view.setWebViewClient(cWebClient);
          } catch (Exception e) {
            Log.e(LOG_TAG, "Got unkown error during setting webview in activity", e);
          }
        }
      });
    } catch (Exception e) {
      Log.e(LOG_TAG, "Got unkown error during passing to UI Thread", e);
    }

    if (success) {
      callbackContext.success();
      return true;
    } else {
      callbackContext.error("Invalid Command");
      return false;
    }
  }
}
