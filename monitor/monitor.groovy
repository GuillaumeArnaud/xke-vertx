import org.vertx.groovy.core.http.RouteMatcher

RouteMatcher routeMatcher = new RouteMatcher()

// route to the index file
routeMatcher.get("/") { req ->
    // TODO load the file 'monitor/index.html
}

// route to the javascript files in ./js directory
routeMatcher.get("/js/:script") { req ->
    // TODO load the javascript 'js/:script
}

// create the http server
def server = vertx.createHttpServer()
server.requestHandler(routeMatcher.asClosure())

// TODO attach a sockJS server to the http server and bridge it to "/monitor-evt-bus" uri.

// declare the event bus
def evtBus = vertx.eventBus

// TODO periodically send to 'mongostat.count' the count of element in mongodb. this event bus is bridged to sockjs.

// start the http server
server.listen(8095, "localhost")
