import org.vertx.groovy.core.http.RouteMatcher

RouteMatcher routeMatcher = new RouteMatcher()

// route to the index file
routeMatcher.get("/") { req ->
    req.response.sendFile "monitor/index.html"
}

// route to the javascript files in ./js directory
routeMatcher.get("/js/:script") { req ->
    def script = req.params["script"]
    req.response.sendFile "monitor/js/$script"
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
    evtBus.send('xke.cache', [action: 'count', collection: 'cache'], { msg ->
        evtBus.send('mongostat.count', [count: msg.body.count])
    })
})

// start the http server
server.listen(8095, "localhost")
