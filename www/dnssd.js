/*
 * Bonjour DNS-SD plugin for Cordova.
 * Allows browsing and resolving of local ad-hoc services
 *
 * jarnoh@komplex.org - March 2012
 *
 */


var resolveQueue = [];
var isResolving = false;

var RESOLVE_TIMEOUT = 5000;//ms

function processResolveQueue() {
    if (isResolving || resolveQueue.length === 0){
        return;
    }

    isResolving = true;

    queueItem = resolveQueue.shift();

    function resumeProcessingQueue() {
        isResolving = false;
        processResolveQueue();
    }

    var resolveFailed = function(result){
        console.error("Plugin cordova-dnssd, service resolve failed : ",result);
    }

    var resolveTimeOut = setTimeout(function(){
        console.error("Service resolve timed out:",queueItem.serviceName);
        resumeProcessingQueue();
    },RESOLVE_TIMEOUT);

    function success(result)
    {
        clearTimeout(resolveTimeOut);
        if(result.serviceResolved){
            setTimeout(function() {
                // Defer callback call to detach execution context.
                queueItem.callback(result.hostName, result.port, result.serviceName, result.regType, result.domain);
            }, 0);
        } else{
            resolveFailed(result);
        }

        resumeProcessingQueue();
    }

    cordova.exec(success, resolveFailed, "fi.peekpoke.cordova.dnssd", "resolve", [queueItem.serviceName, queueItem.regType, queueItem.domain]);
}

function DNSSD()
{
}

DNSSD.prototype.stopBrowsing = function(callback) {
    resolveQueue.length = 0;
    var stopBrowsingFailed = function(result){
        console.error("Plugin cordova-dnssd, service stopBrowsing failed : ",result);
    }
    cordova.exec(callback, stopBrowsingFailed, "fi.peekpoke.cordova.dnssd", "stopBrowsing", []);
}

DNSSD.prototype.browse=function(regType, domain, serviceFound, serviceLost) {
    function success(result)
    {
        if(result.serviceFound)
            serviceFound(result.serviceName, result.regType, result.domain, result.moreComing);
        else if(result.serviceLost)
            serviceLost(result.serviceName, result.regType, result.domain, result.moreComing);
    }

    var browsingFailed = function(result){
        console.error("Plugin cordova-dnssd, service browse failed : ",result);
    }
    cordova.exec(success, browsingFailed, "fi.peekpoke.cordova.dnssd", "browse", [regType, domain]);
}

DNSSD.prototype.resolve=function(serviceName, regType, domain, serviceResolved) {
    resolveQueue.push({
        callback: serviceResolved,
        serviceName: serviceName,
        regType: regType,
        domain: domain
    });

    processResolveQueue()
}

module.exports = new DNSSD();

/*

API for callbacks:

function serviceResolved(hostName, port, serviceName, regType, domain)
function serviceFound(serviceName, regType, domain, moreComing)
function serviceLost(serviceName, regType, domain, moreComing)

*/
