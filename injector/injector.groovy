def alphabet = (('A'..'Z') + ('0'..'9')).join()
def randomString = { size ->
    new Random().with {
        (1..size).collect { alphabet[nextInt(alphabet.length())] }.join()
    }
}

10.times {
    def client = vertx.createHttpClient(host: "localhost", port: 8090)
    client.exceptionHandler { ex -> println ex }

    // call a put request with a random key and value
    def key = randomString(10)
    def value = randomString(20)

    println "put..."
    def request = client.put("/$key/$value/") { resp ->
        println "put $key:$value"
    }
    request.headers["Content-Length"] = 0
    request.end()

    sleep(10)

    println "get..."
    // call a get request with the preceding key
    client.getNow("/$key/") { resp ->
        try {
            // display the body of the response. it should be equal to value
            resp.bodyHandler { body ->
                if (!body.toString().equals(value)) println "Response ($body) different from value ($value)"
                else println "get $key=$value"
            }
        } catch (Exception e) {e.printStackTrace()}
    }

}

println "end"
