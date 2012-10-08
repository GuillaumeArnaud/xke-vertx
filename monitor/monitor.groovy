import org.vertx.groovy.core.http.RouteMatcher

def routeMatcher = new RouteMatcher()

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

// attach a sockJS server to the http server and bridge it to "/monitor-evt-bus" uri.
def config = ["prefix": "/monitor-evt-bus"]
vertx.createSockJSServer(server).bridge(config, [[:]], [[:]])

// send mongostat to the event bus. this event bus is bridged to sockjs.
vertx.setPeriodic(10000, {
    vertx.eventBus.send("mongostat", ["value": "coucou"])
}
)

// start the http server
server.requestHandler(routeMatcher.asClosure()).listen(8095, "localhost")
