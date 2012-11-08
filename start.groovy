// configure mongo (default port is 27017) to address "xke:cache" and base "xke"
def mongoConf = [
        "address": "xke.cache",
        "host": "localhost",
        "port": 27017,
        "db_name": "xke"
]

// configure replica conf
def replicaConf = [
        "host": "localhost",
        "port": 54034,
        "server": false
]

container.with {
    deployModule('vertx.mongo-persistor', mongoConf, 1)
    deployVerticle("cache/cache.groovy")
    deployVerticle("monitor/monitor.groovy")
}