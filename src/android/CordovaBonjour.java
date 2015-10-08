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

public class CordovaBonjour extends CordovaPlugin {
    String tag = "dnssd";
    Stack<NsdManager.DiscoveryListener> listenerStack = new Stack<NsdManager.DiscoveryListener>();
    PluginResult result;

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
            Log.d(tag, "Start discovering services of type "+serviceType);
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(tag, "Service "+service+" of type "+service.getServiceType()+" found.");
            JSONObject serviceJSON = new JSONObject();
            try{
                serviceJSON = new JSONObject();
                serviceJSON.put(new String("serviceFound"), new Boolean(true));
                serviceJSON.put("serviceName", service.getServiceName());
                serviceJSON.put("regType",service.getServiceType());
                serviceJSON.put("domain","");//NOTE : such a property doesn't exist in class NsdServiceInfo`
                serviceJSON.put("moreComing",null);
            }
            catch (JSONException e){
                Log.e(tag, "JSON execption ");
            }
            result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return;
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.e(tag, "Service lost " + service);
            JSONObject serviceJSON = new JSONObject() ;
            try{
                serviceJSON = new JSONObject();
                serviceJSON.put("serviceLost", new Boolean(true));
                serviceJSON.put("regType",service.getServiceType());
                serviceJSON.put("serviceName",service.getServiceName());
            }
            catch (JSONException e){
                Log.e(tag, "JSON Exception");
            }
            result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return;
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(tag, "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(tag, "Discovery failed: Error code:" + errorCode);
            getNsdManager().stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(tag, "Discovery failed: Error code:" + errorCode);
            getNsdManager().stopServiceDiscovery(this);
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
            Log.e(tag, "Resolve failed" + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(tag, "Resolve Succeeded. " + serviceInfo);
            //create a JSON object to send the results
            JSONObject serviceJSON = new JSONObject();
            try{
                serviceJSON.put("hostName" ,serviceInfo.getHost().getHostName() );
                serviceJSON.put("port", serviceInfo.getPort());
                serviceJSON.put("serviceName",serviceInfo.getServiceName());
                serviceJSON.put("regType",serviceInfo.getServiceType());
                serviceJSON.put("domain","");
            }
            catch (JSONException e){
                Log.e(tag,"JSON Execption ");
            }
            result = new PluginResult(PluginResult.Status.OK,serviceJSON);
            result.setKeepCallback(true);
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
        // Instantiate a new DiscoveryListener
    }

    private void stopServiceDiscovery(CallbackContext callbackContext){
        Log.d(tag,"Stoping all discovery services");
        while (!listenerStack.empty()){
            Log.d(tag,"stopping service listener "+listenerStack.peek());
            getNsdManager().stopServiceDiscovery(listenerStack.pop());
        }
    }

    private void resolveService(String serviceName, String serviceType, CallbackContext callbackContext){
        DnssdResolveListener myResolveListener = new DnssdResolveListener(callbackContext);
        NsdServiceInfo serviceToResolve = new NsdServiceInfo();
        serviceToResolve.setServiceName(serviceName);
        serviceToResolve.setServiceType(serviceType);
        getNsdManager().resolveService(serviceToResolve,myResolveListener);
    }
}
