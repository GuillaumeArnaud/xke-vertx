import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.java.core.json.impl.Json
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

def routeMatcher = new RouteMatcher()

// declare the logger vertx
def logger = container.logger

// declare the event bus
def evtBus = vertx.eventBus

// TODO declare the share map 'cache.leve2'
def cacheL2 = vertx.sharedData.getMap('cache.level2')

// declare the TTL_L1 and TTL_L2 in milliseconds
int TTL_L1 = 30 * 1000
int TTL_L2 = 3 * 1000

// declare the port of the server
int port = 8090

// declare the hit counter (AtomicLong)
AtomicLong hitsL1 = new AtomicLong(0)
AtomicLong hitsL2 = new AtomicLong(0)

// write a response to the request with chunk disable and Content-Length header
def response = { req, value ->
    req.response.chunked = false
    req.response.headers["Content-Length"] = value.length()
    req.response.end(value)
}

// implement the get handler ('http://localhost:8080/key/value/')
routeMatcher.get("/:key/:value/") { req ->
    // send an "update" action to the collection "cache" with attributes "key" and "value" from request parameters
    def key = req.params.key
    def value = req.params.value
    def thread = Thread.currentThread().name
    def hostName = InetAddress.getLocalHost().getHostName()

    // add a timer for removing this element after the TTL divides per 10
    vertx.setTimer(TTL_L2) { cacheL2.remove(key)}

    // build the save message for mongo-persistor
    def msg = [
            action: "save",
            collection: "cache",
            document: [
                    _id: key,
                    value: value,
                    date: System.currentTimeMillis(),
                    port: port,
                    thread: thread,
                    hostname: hostName
            ]
    ]

    // add to shared map
    cacheL2[key] = Json.encode(msg.document)

    evtBus.send("xke.cache", msg) { message ->
        def status = (message.body.status ?: "nok")
        logger.info "[$thread] put=$status"

        // write the response
        req.response.chunked = false
        req.response.headers["Content-Length"] = status.length()
        req.response.end(status)
    }

}

// implement the get handler ('http://localhost:8080/key/')
routeMatcher.get("/:key/") { req ->
    def key = req.params.key
    def thread = Thread.currentThread().name

    // check that the value is in cache
    if (cacheL2.containsKey(key)) {
        def valueEncoded = cacheL2.get(key)
        def value = Json.decodeValue(valueEncoded, LinkedHashMap.class)
        logger.info "[${thread}][level2][${thread.equals(value.thread)}][key=$key][value=$value]"

        // send the stats to 'mongostat.l2.hit'
        vertx.eventBus.send("mongostat.l2.hit", [hit: hitsL2.incrementAndGet()])

        response(req, valueEncoded)
    } else {
        // send a "find" action from the collection "cache" with attribute "key"
        evtBus.send("xke.cache", [action: "find", collection: "cache", matcher: [_id: key]]) { message ->
            logger.info "[$thread][level1][key=$key][value=${message.body}]"

            if(message.body.results.size() > 0) {
                // send the stats to 'mongostat.l1.hit'
                vertx.eventBus.send("mongostat.l1.hit", [hit: hitsL1.incrementAndGet()])
            }

            def value = ""
            message.body.results.each { result ->
                value = Json.encode(result)
            }

            // write the found message to http response
            response(req, value)
        }
    }
}

routeMatcher.noMatch { req ->
    req.response.end("no match to path '${req.path}' with parameters '${req.params}' and method '${req.method}' .")
}

// remove periodically elements which are too old in mongo cache
vertx.setPeriodic(1000, {
    evtBus.send("xke.cache", [action: "delete",
            collection: "cache",
            matcher: [date: ["\$lte": System.currentTimeMillis() - TTL_L1]]]) {
        logger.debug "remove ${it.body.number} elements"
    }
})

// start the http to the declared port
logger.info "start server"
vertx.createHttpServer().requestHandler(routeMatcher.asClosure()).listen(port, "localhost")