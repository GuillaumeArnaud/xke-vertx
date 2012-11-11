import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.java.core.json.impl.Json
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

def routeMatcher = new RouteMatcher()

// declare the logger vertx
def logger = container.logger

// declare the event bus
def evtBus = vertx.eventBus

// declare the TTL_L1 and TTL_L2 in milliseconds
int TTL_L1 = 30 * 1000
int TTL_L2 = 3 * 1000

// TODO retrieve the port of the server from configuration defined in 'start.groovy'
// int port =

// declare the hit counter (AtomicLong)
AtomicLong hitsL1 = new AtomicLong(0)
AtomicLong hitsL2 = new AtomicLong(0)

// TODO implement the get handler ('http://localhost:8080/key/') which only return the key

logger.info "start server"
// TODO start the http to the declared port