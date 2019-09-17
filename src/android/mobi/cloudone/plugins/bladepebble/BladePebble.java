/**
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2013, Maciej Nux Jaros
 */
package mobi.cloudone.plugins.bladepebble;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.pm.PackageManager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

public class BladePebble extends CordovaPlugin {

    private static final String LOG_TAG = "BladePebble";
    private CordovaWebView cwv;
    private WebView wv;
    private PebbleJavaScriptInterface pebbleJavaScriptInterface;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        pebbleJavaScriptInterface = new PebbleJavaScriptInterface(this);

        cwv = webView;
        wv = (WebView)webView.getEngine().getView();
        // your init code here

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setBuiltInZoomControls(true);

        wv.addJavascriptInterface(pebbleJavaScriptInterface, "PebbleInterface");

    }

    /**
     * Constructor.
     */
    public BladePebble() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Resources.TAG, "onActivityResult: " + data.getExtras());
        
        Bundle bundle = data.getBundleExtra(Resources.BLADE_INTENT_BUNDLE_RESPONSE);
        if (resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            try {
                JSONArray arr = new JSONArray();
                buildJSONObjectFromBundle(arr, extras);

                Log.d(Resources.TAG, "=================================");
                Log.d(Resources.TAG, arr.toString(4));
                Log.d(Resources.TAG, "=================================");

                lastJsonResponseStr = arr.toString(4);
                cwv.loadUrl("javascript:paymentPebbleResponse()");
                
            } catch (Exception ex) {
                Log.e(Resources.TAG, ex.getMessage(), ex);
            }
        }
    }

    public class PebbleJavaScriptInterface {
        private CordovaPlugin that;

        public PebbleJavaScriptInterface(CordovaPlugin cp) {
            this.that = cp;
        }

        @JavascriptInterface
        public void launchPaymentPebble(String json) {
            Log.d(Resources.TAG, "LAUNCH PEBBLE: " + json);
            try {
                JSONObject data = new JSONObject(json); //Convert from string to object, can also use JSONArray

                HashMap<String, Object> extrasMap = new HashMap<String, Object>();
                populateExtrasIntoToMap(data, data.names(), extrasMap);

                Log.d(Resources.TAG, "Parsed json: " + extrasMap.toString());

                Intent intent = new Intent();
                intent.setClassName(Resources.BLADE_APP_URL, Resources.BLADE_APP_CLASS);

                Bundle extras = new Bundle();
                intent.putExtra(Resources.BLADE_INTENT_BUNDLE, extras);

                populateIntentData(extras, extrasMap);

                Log.d(Resources.TAG, "Sending intent data: " + intent.getExtras().toString());

                that.cordova.startActivityForResult(that, intent, 1);

            } catch (Exception ex) {
                Log.e(Resources.TAG, ex.getMessage(), ex);
            }
        }

        @JavascriptInterface
        public void initialisePaymentPebble(String json) {
            Log.d(Resources.TAG, "INIT PEBBLE: " + json);
            try {
                JSONObject data = new JSONObject(json); //Convert from string to object, can also use JSONArray

                HashMap<String, Object> extrasMap = new HashMap<String, Object>();
                populateExtrasIntoToMap(data, data.names(), extrasMap);

                Log.d(Resources.TAG, "Parsed json: " + extrasMap.toString());

                Intent intent = new Intent(Resources.BLADE_SVC_COMMAND);
                intent.setClassName(Resources.BLADE_APP_URL, Resources.BLADE_SVC_CLASS);

                Bundle extras = new Bundle();
                extras.putString(Resources.BLADE_INTENT_NAMESPACE, getApplicationContext().getPackageName());
                intent.putExtra(Resources.BLADE_INTENT_BUNDLE, extras);

                populateIntentData(extras, extrasMap);

                Log.d(Resources.TAG, "Sending intent data: " + intent.getExtras().toString());

                that.cordova.getActivity().startService(intent);
            } catch (Exception ex) {
                Log.e(Resources.TAG, ex.getMessage(), ex);
            }
        }

        @JavascriptInterface
        public String getJsonResponse() {
            return lastJsonResponseStr;
        }

        /**
         * Method to parse JSON structure into a similar HashMap style structure
         *
         * @param srcObj
         * @param arr
         * @param extrasMap
         */
        private void populateExtrasIntoToMap(JSONObject srcObj, JSONArray arr, HashMap<String, Object> extrasMap) throws Exception {
            if (arr != null && arr.length() > 0) {
                Log.d(Resources.TAG, "Array not null...");
                for (int i = 0; i < arr.length(); i++) {
                    Object node = arr.get(i);
                    Log.d(Resources.TAG, "iterating through array: " + node.toString());
                    if (node instanceof JSONObject) {
                        Log.d(Resources.TAG, "found a json object...");
                        JSONObject obj = (JSONObject) node;
                        populateExtrasIntoToMap(obj, obj.names(), extrasMap);
                    } else if (node instanceof String) {
                        Log.d(Resources.TAG, "found a json value...");
                        String key = (String) node;
                        String keyName = srcObj.getString(key);

                        extrasMap.put(key, keyName);
                    }
                }
            }
        }

        /**
         * Method to convert a HashMap into an intent data structure
         *
         * @param bundle
         * @param extrasMap
         */
        private void populateIntentData(Bundle bundle, HashMap<String, Object> extrasMap) throws Exception {
            Iterator<String> it = extrasMap.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object val = extrasMap.get(key);
                if (val instanceof String) {
                    bundle.putString(key, val+"");
                } else if (val instanceof HashMap) {
                    HashMap<String, Object> subMap = (HashMap<String, Object>) val;
                    Bundle subBundle = new Bundle();
                    populateIntentData(subBundle, subMap);

                    if (bundle != null) {
                        throw new Exception("Attempted to put extras 2 levels down which is not permitted.");
                    }
                }
            }
        }
    }


    

}