import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.java.core.json.impl.Json
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// configure mongo (default port is 27017) to address "xke:cache" and base "xke"
def mongoConf = [
        "address": "xke.cache",
        "host": "localhost",
        "port": 27017,
        "db_name": "xke"
]

def routeMatcher = new RouteMatcher()

// declare the logger vertx
def logger = container.logger

// declare the mongo persistor mods. That will download the module at the first start and will create a 'mods' directory
// to the working directory.
container.with {
    deployModule('vertx.mongo-persistor-v1.0', mongoConf, 1)
}

// declare the event bus
def evtBus = vertx.eventBus

// declare the share map
def cacheL2 = vertx.sharedData.getMap('cache.level2')

// declare the TTL in milliseconds
int TTL = 30 * 1000

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

// implement the put handler ('http://localhost:8080/key/value')
routeMatcher.get("/:key/:value/") { req ->
    // send an "update" action to the collection "cache" with attributes "key" and "value" from request parameters
    def key = req.params.key
    def value = req.params.value
    def thread = Thread.currentThread().name

    def msg = [
            action: "save",
            collection: "cache",
            document: [
                    _id: key,
                    value: value,
                    date: System.currentTimeMillis(),
                    port: port,
                    thread: thread
            ]
    ]

    // add to shared map
    cacheL2[key] = Json.encode(msg.document)

    // add a timer for removing this element after the TTL
    vertx.setTimer(TTL) { cacheL2.remove(key)}

    evtBus.send("xke.cache", msg) { message ->
        def status = (message.body.status ?: "nok")
        logger.info "[$thread] put=$status"

        // write the response
        req.response.chunked = false
        req.response.headers["Content-Length"] = "ok".length()
        req.response.end("ok")

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
        vertx.eventBus.send("mongostat.l2.hit",[hit:hitsL2.incrementAndGet()])

        response(req, valueEncoded)
    } else {
        // send a "find" action from the collection "cache" with attribute "key"
        evtBus.send("xke.cache", [action: "find", collection: "cache", matcher: [_id: key]]) { message ->
            logger.info "[$thread][level1][key=$key][value=${message.body}]"

            // send the stats to 'mongostat.l1.hit'
            vertx.eventBus.send("mongostat.l1.hit",[hit:hitsL1.incrementAndGet()])

            def value = ""
            message.body.results.each { result ->
                value = "$value $result \n"
            }

            // write the found message to http response
            response(req, value)
        }
    }
}

routeMatcher.noMatch { req ->
    req.response.end("no match to path '${req.path}' with parameters '${req.params}' and method '${req.method}' .")
}

// return the current hit count for l1 ('mongostat.l1.hit')
vertx.eventBus.registerHandler("mongostat.l1.hit") { message ->
    message.reply([hit:hitsL1.get()])
}

// return the current hit count for l2 ('mongostat.l2.hit')
vertx.eventBus.registerHandler("mongostat.l2.hit") { message ->
    message.reply([hit:hitsL2.get()])
}

// remove elements which are too old
vertx.setPeriodic(2000, {
    evtBus.send("xke.cache", [action: "delete",
                            collection: "cache",
                            matcher: [date: ["\$lte": System.currentTimeMillis() - 30 * 1000]]]) {
        logger.debug "remove ${it.body.number} elements"
    }
})

// start the http to the declared port
logger.info "start server"
vertx.createHttpServer().requestHandler(routeMatcher.asClosure()).listen(port, "localhost")