
def logger = container.logger

def alphabet = (('A'..'Z') + ('0'..'9')).join()
def randomString = { size ->
    new Random().with {
        (1..size).collect { alphabet[nextInt(alphabet.length())] }.join()
    }
}

def random = new Random()

logger.info "start injection "
vertx.setPeriodic(2000) {
//10.times {
    def client = vertx.createHttpClient(host: "localhost", port: 8090)
    client.exceptionHandler { ex -> println ex }

    // call a put request with a random key and value
    def key = randomString(10)
    def value = randomString(20)

    logger.info "put"
    client.getNow("/$key/$value/") { resp ->
        println "put $key=$value"
    }

    vertx.setTimer(random.with {random.nextInt(6000)}) {
        client.getNow("/$key/") { respGet ->
            try {
                // display the body of the response. it should be equal to value
                respGet.bodyHandler { result ->
                    if (!result.toString().equals(value)) println "Response ($result) different from value ($value)"
                    else println "get $key=$value"
                }
                // close the client connection
                client.close()
            } catch (Exception e) {e.printStackTrace()}
        }
    }

}
