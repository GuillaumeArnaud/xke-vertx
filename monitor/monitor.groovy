import org.vertx.groovy.core.http.RouteMatcher

// configure mongo (default port is 27017) to address "xke:cache" and base "xke"
def mongoConf = [
        "address": "xke.cache.monitor",
        "host": "localhost",
        "port": 27017,
        "db_name": "xke"
]

// declare the mongo persistor mods. That will download the module at the first start and will create a 'mods' directory
// to the working directory.
container.with {
    deployModule('vertx.mongo-persistor-v1.0', mongoConf, 1)
}

RouteMatcher routeMatcher = new RouteMatcher()

// route to the index file
routeMatcher.get("/") { req ->
    req.response.sendFile "index.html"
}

// route to the javascript files in ./js directory
routeMatcher.get("/js/:script") { req ->
    def script = req.params["script"]
    req.response.sendFile "js/$script"
}

// create the http server
def server = vertx.createHttpServer()
server.requestHandler(routeMatcher.asClosure())

// attach a sockJS server to the http server and bridge it to "/monitor-evt-bus" uri.
def config = ["prefix": "/monitor-evt-bus"]
vertx.createSockJSServer(server).bridge(config, [[:]], [[:]])

// declare the event bus
def evtBus = vertx.eventBus

// send mongostat to the event bus. this event bus is bridged to sockjs.
vertx.setPeriodic(2000, {
    evtBus.send('xke.cache.monitor', [action: 'count', collection: 'cache'], { msg ->
        evtBus.send('mongostat.count', [count: msg.body.count])
    })
})


// start the http server
server.listen(8095, "localhost")
