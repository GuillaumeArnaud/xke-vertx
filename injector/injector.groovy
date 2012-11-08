import com.mongodb.util.JSON

def alphabet = (('A'..'Z') + ('0'..'9')).join()
def randomString = { size ->
    new Random().with {
        (1..size).collect { alphabet[nextInt(alphabet.length())] }.join()
    }
}

def random = new Random()

vertx.setPeriodic(2000) {
//10.times {
    def client = vertx.createHttpClient(host: "localhost", port: 8090)
    client.exceptionHandler { ex -> println ex }

    // call a put request with a random key and value
    def key = randomString(10)
    def value = randomString(20)

    // TODO call a client request on path /#key#/#value#/
    client.getNow("/$key/$value/") { resp ->
        println "put $key=$value"
    }

    vertx.setTimer(random.with {random.nextInt(6000)}) {
        // TODO call a client request on path /#key#/ for getting the precedent value and check that is the expected value
        client.getNow("/$key/") { respGet ->
            try {
                // display the body of the response. it should be equal to value
                respGet.bodyHandler { body ->
                    def result = JSON.parse(body.toString()).value
                    if (!result.toString().equals(value)) println "Response ($result) different from value ($value)"
                    else println "get $key=$value"
                }
                // close the client connection
                client.close()
            } catch (Exception e) {e.printStackTrace()}
        }
    }

}
