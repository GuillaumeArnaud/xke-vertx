import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.net.NetSocket
import org.vertx.groovy.core.net.NetServer
import org.vertx.java.core.json.JsonObject
import org.vertx.java.core.json.impl.Json
import org.vertx.groovy.core.net.NetClient

// configuration
String host = container.config["host"] ?: "localhost"
int port = container.config.port ?: 54540
def isServer = container.config["server"]

if (isServer) {
    // create the tcp server
    NetServer netServer = vertx.createNetServer()

    // add an handler for client connections
    netServer.connectHandler({ socket ->
        socket.dataHandler { buffer ->
            // insert the message to mongo
            def message = buffer.toString()
            def json = new JsonObject(message)
            vertx.eventBus.send("xke.cache", json) { msg ->
                def status = (msg.body.status ?: "nok")
                println "replication: status=$status"
            }
        }
    }).listen(port, "localhost")
} else {
    vertx.eventBus.registerHandler("replica") { message ->
        // message contains the save operation to replicated

        // create a tcp client
        NetClient client = vertx.createNetClient()

        // connect to tcp://host:port
        client.connect(port, host) { socket ->
            // send the request
            socket << new Buffer(Json.encode(message.body))

            // close the socket and client connection
            ((NetSocket) socket).closedHandler {
                client.close()
                println "socket closed"
            }
            ((NetSocket) socket).close()
        }
    }
}

println "replica module ready on $host:$port, server mod=$isServer"

