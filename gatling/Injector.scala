package basic

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import java.util.UUID

class Injector extends Simulation {

    def apply = {

            val httpConf = httpConfig.baseURL("http://localhost:8090")

            val scn = scenario("Scenario")
            .during(10 seconds){
            feed(keyValueFeeder)
                .exec(http("put")
                        .get("/${key}/${value}/")
                        .check(status.is(200))
                        .check(regex("ok"))
                )
                //.pause(2 milliseconds)
                .exec(http("get")
                        .get("/${key}/")
                        .check(status.is(200))
                )
            }

            List(scn.configure.users(100)
                    .ramp(10)
                    .protocolConfig(httpConf)

            )
    }

val keyValueFeeder = new Feeder {
    // always return true as this feeder can be polled infinitively
    override def hasNext = true

    override def next: Map[String, String] = {
         Map(
            "value" -> UUID.randomUUID().toString(),
            "key" -> UUID.randomUUID().toString()
            )
        }
    }

}
