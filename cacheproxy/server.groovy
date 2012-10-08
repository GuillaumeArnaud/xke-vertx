import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.core.buffer.Buffer

// configure mongo (default port is 27017) to address "xke:cache" and base "xke"
def mongoConf = [
        "address": "xke:cache",
        "host": "localhost",
        "port": 27017,
        "db_name": "xke"
]

def routeMatcher = new RouteMatcher()

// declare the mongo persistor mods. That will download the module at the first start and will create a 'mods' directory
// to the working directory.
container.with {
    deployModule('vertx.mongo-persistor-v1.0', mongoConf)
}

// declare the event bus
def evtBus = vertx.eventBus

// implement the put handler ('http://localhost:8080/key/value')
routeMatcher.put("/:key/:value/") { req ->
    println "put ${req.params.key}=${req.params.value}"

    // retrieve the payload of the request
    def body = new Buffer(0)
    req.dataHandler { buffer -> body << buffer }

    req.endHandler {
        // send an "update" action to the collection "cache" with attributes "key" and "value" from request parameters
        def msg = [
                action: "save",
                collection: "cache",
                document: [
                        key: req.params.key,
                        value: req.params.value
                ]
        ]

        evtBus.send("xke:cache", msg) { message ->
            println "snd=${message.body}"
        }
    }
    req.response.end("ok")
}

// implement the get handler ('http://localhost:8080/key/')
routeMatcher.get("/:key/") { req ->
    println "get ${req.params.key}"

        // send a "find" action from the collection "cache" with attribute "key"
        evtBus.send("xke:cache", [action: "find", collection: "cache", matcher: [key: req.params.key]]) { message ->
            println "rcv=${message.body}"
            def value = message.body.results[0].value

            // write the found message to http response
            req.response.headers["Content-Length"] = value.length()
            req.response.end(value)
        }
}

routeMatcher.noMatch { req ->
    println "no match ${req.params} ${req.method} ${req.path}"
}

// start the http to 8080
println "start server"
vertx.createHttpServer().requestHandler(routeMatcher.asClosure()).listen(8090, "localhost")