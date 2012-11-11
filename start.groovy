// configure cache verticle
def cacheConf = [
        port: 8091
]

// configure mongo (default port is 27017) to address "xke.cache" and base "xke"
def mongoConf = [
        "address": "xke.cache",
        "host": "localhost",
        "port": 27017,
        "db_name": "xke2"
]

// configure replica conf
def replicaConf = [
        "host": "localhost",
        "port": 54034,
        "server": false
]

def logger = container.logger

container.with {
    // TODO add the verticles and modules to the container
    deployVerticle("cache/cache.groovy", cacheConf, 1) {
        logger.info "verticle cache started"
    }
    deployModule("vertx.mongo-persistor-v1.2", mongoConf)
}