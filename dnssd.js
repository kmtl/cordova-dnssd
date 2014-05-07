/*
 * Bonjour DNS-SD plugin for Cordova.
 * Allows browsing and resolving of local ad-hoc services
 *
 * jarnoh@komplex.org - March 2012
 *
 */


var resolveQueue = [];
var isResolving = false;

function processResolveQueue() {
    if (isResolving || resolveQueue.length === 0)
        return;

    isResolving = true;

    queueItem = resolveQueue.shift();

    function success(result)
    {
        if(result.serviceResolved)
            setTimeout(function() {
                // Defer callback call to detach execution context.
                queueItem.callback(result.hostName, result.port, result.serviceName, result.regType, result.domain);
            }, 0);

        isResolving = false;
        processResolveQueue()
    }

    cordova.exec(success, function(){}, "fi.peekpoke.cordova.dnssd", "resolve", [queueItem.serviceName, queueItem.regType, queueItem.domain]);
}

function DNSSD()
{
}

DNSSD.prototype.browse=function(regType, domain, serviceFound, serviceLost) {
    console.log("browse "+regType);

    function success(result)
    {
        if(result.serviceFound)
            serviceFound(result.serviceName, result.regType, result.domain, result.moreComing);
        if(result.serviceLost)
            serviceLost(result.serviceName, result.regType, result.domain, result.moreComing);
    }

    cordova.exec(success, function(){}, "fi.peekpoke.cordova.dnssd", "browse", [regType, domain]);
}

DNSSD.prototype.resolve=function(serviceName, regType, domain, serviceResolved) {
    console.log("resolve "+serviceName);

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
