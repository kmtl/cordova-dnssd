/**
 * Cordova plugin dnssd, Android Implementation
 * NOTE: in android Implementation, a dnssd service (class NsdServiceInfo) doesn't have a domain property
 */
package com.coqsenpate.cordova;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Stack;
import java.lang.RuntimeException;

public class CordovaBonjour extends CordovaPlugin {
    private static final String TAG = "dnssd";
    private Stack<NsdManager.DiscoveryListener> listenerStack = new Stack<NsdManager.DiscoveryListener>();

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException{
        if (action.equals("browse")){
            String serviceType = args.getString(0);
            String serviceDomain = args.getString(1);
            discoverService(serviceType,callbackContext);
            return true;
        }
        else if (action.equals("stopBrowsing")){
            stopServiceDiscovery(callbackContext);
            return true;
        }
        else if (action.equals("resolve")){
            String serviceName = args.getString(0);
            String serviceType = args.getString(1);
            String serviceDomain = args.getString(2);//NOTE : can not be used since Android dnssd service class NsdServiceInfo doesn't have such a property
            resolveService(serviceName, serviceType, callbackContext);
            return true;
        }
        else{
            PluginResult result = new PluginResult(PluginResult.Status.INVALID_ACTION, action);
            return false;
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    ////////////////    UTIL CLASS  ANF FUNCTION DEFINITION          //////////
    //////////////////////////////////////////////////////////////////////////////
    private NsdManager getNsdManager(){
        return (NsdManager)cordova.getActivity().getSystemService(Context.NSD_SERVICE);
    }

    //a service discovery listener
    private class DnssdServiceListener implements NsdManager.DiscoveryListener {
        private CallbackContext callbackContext;
        DnssdServiceListener(CallbackContext callbackContext){
            this.callbackContext = callbackContext;
        }
        //called when the service begins
        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "Start discovering services of type "+serviceType);
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(TAG, "Service found : "+service+" of type "+service.getServiceType()+" found.");

            //create JSONObject to send as result
            JSONObject serviceJSON = new JSONObject();
            try{
                serviceJSON.put("serviceFound", true);
                serviceJSON.put("serviceName", service.getServiceName());
                serviceJSON.put("regType",service.getServiceType());
                serviceJSON.put("domain","");//NOTE : such a property doesn't exist in class NsdServiceInfo`
                serviceJSON.put("moreComing",null);
            }
            catch(JSONException e){
                throw new RuntimeException(e);
            }

            //send result keeping callbackContext
            PluginResult result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return;
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.d(TAG, "Service lost " + service);

            //create JSONObject to send as result
            JSONObject serviceJSON = new JSONObject() ;
            try{
                serviceJSON = new JSONObject();
                serviceJSON.put("serviceLost", true);
                serviceJSON.put("regType",service.getServiceType());
                serviceJSON.put("serviceName",service.getServiceName());
            }
            catch(JSONException e){
                throw new RuntimeException(e);
            }

            //send result keeping callbackContext
            PluginResult result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return;
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.d(TAG, "Discovery stopped: " + serviceType);
            PluginResult result = new PluginResult(PluginResult.Status.OK,0);
            callbackContext.sendPluginResult(result);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            getNsdManager().stopServiceDiscovery(this);
            JSONObject serviceJSON = new JSONObject();
            try{
                serviceJSON.put("error",errorCode);
                serviceJSON.put("startDiscoveryFailed",true);
            }
            catch(JSONException e){
                throw new RuntimeException(e);
            }
            PluginResult result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            callbackContext.sendPluginResult(result);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            getNsdManager().stopServiceDiscovery(this);
            JSONObject serviceJSON = new JSONObject();
            try{
                serviceJSON.put("error",errorCode);
                serviceJSON.put("stopDiscoveryFailed",true);
            }
            catch(JSONException e){
                throw new RuntimeException(e);
            }
            PluginResult result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            callbackContext.sendPluginResult(result);
        }
    }

    //////////////////////////////
    // a resolve Listener
    private class DnssdResolveListener implements NsdManager.ResolveListener {
        CallbackContext callbackContext;
        DnssdResolveListener(CallbackContext callbackContext){
            this.callbackContext = callbackContext;
        }
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails.  Use the error code to debug.
            Log.e(TAG, "Resolve failed" + errorCode);
            JSONObject serviceJSON = new JSONObject();
            try{
                serviceJSON.put("error",errorCode);
                serviceJSON.put("serviceResolved",new Boolean(false));
            }
            catch(JSONException e){
                throw new RuntimeException(e);
            }

            PluginResult result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            callbackContext.sendPluginResult(result);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

            //create a JSON object to send the results
            JSONObject serviceJSON = new JSONObject();
            try{
                serviceJSON.put("serviceResolved", new Boolean(true));
                serviceJSON.put("hostName" ,serviceInfo.getHost().getHostName() );
                serviceJSON.put("port", serviceInfo.getPort());
                serviceJSON.put("serviceName",serviceInfo.getServiceName());
                serviceJSON.put("regType",serviceInfo.getServiceType());
                serviceJSON.put("domain","");
            }
            catch(JSONException e){
                throw new RuntimeException(e);
            }

            PluginResult result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            callbackContext.sendPluginResult(result);
        }
    }


    //////////////////////////////////////////////////////////////////////////////
    /////////////   METHODS CALLED BY EXECUTE FUNCTION              /////////////
    //////////////////////////////////////////////////////////////////////////////

    private void discoverService(String serviceType, CallbackContext callbackContext) {
        DnssdServiceListener myServiceListener = new DnssdServiceListener(callbackContext);
        getNsdManager().discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, myServiceListener);
        listenerStack.push(myServiceListener);
        return;
    }

    private void resolveService(String serviceName, String serviceType, CallbackContext callbackContext){
        DnssdResolveListener myResolveListener = new DnssdResolveListener(callbackContext);
        NsdServiceInfo serviceToResolve = new NsdServiceInfo();
        serviceToResolve.setServiceName(serviceName);
        serviceToResolve.setServiceType(serviceType);
        getNsdManager().resolveService(serviceToResolve,myResolveListener);
        return;
    }

    private void stopServiceDiscovery(CallbackContext callbackContext){
        Log.d(TAG,"Stoping all discovery services");
        while (!listenerStack.empty()){
            Log.d(TAG,"stopping service listener "+listenerStack.peek());
            getNsdManager().stopServiceDiscovery(listenerStack.pop());
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK,0);
        callbackContext.sendPluginResult(result);
        return;
    }

}
