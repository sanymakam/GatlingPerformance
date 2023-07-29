package simulation

import com.github.javafaker.Faker
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * This is a sample simulation for
 * 1. Protocol - Http protocol with post methods
 * 2. Feeders - csv feeders
 * 3. Requests are fired in parallel
 * 3. Request bodies are read from json files which has static and dynamic values
 * 4. Injection mechanism - sophisticated injection mechanism with proper control on RPS/RPM.
 * Script will start with Initial RPS, reaches final RPS over the period of initial duration minutes and holds the final RPS for target duration minutes
 */
class CreateUserScalaSimulation extends Simulation {
  //json files which contains the requests body
  val createUserPayload = "createUser.json";

  //Data feeder which can be csv,json,db,redis,list,array
  val userNameFeeder = Iterator.continually(
    // Random number will be accessible in session under variable "OrderRef"
    Map("userName" -> Faker.instance().name().name())
  )

  val initialLoad = Integer.parseInt(System.getProperty("INITIAL_LOAD", "1"))
  val targetLoad = Integer.parseInt(System.getProperty("TARGET_LOAD", "1"))
  val initialDuration = java.lang.Long.parseLong(System.getProperty("INITIAL_DURATION", "1"))
  val targetDuration = java.lang.Long.parseLong(System.getProperty("TARGET_DURATION", "1"))

  //Protocol used, in this case its http. Along with common headers for all the individual api calls
  val httpProtocol = http
    .baseUrl("https://reqres.in/")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  object CreateUser {
    val createUser = scenario("Create User")
      .repeat(1) {

        feed(userNameFeeder)
          .exec(http("Create Users")
            .post("/api/users")
            .body(ElFileBody(createUserPayload))
            .check(status.is(201)))
      }
  }

  System.out.println("Initial Load = " + initialLoad)
  System.out.println("Target Load = " + targetLoad)
  System.out.println("Initial Ramp up Duration = " + initialDuration + " seconds")
  System.out.println("Duration to hold the target load = " + targetDuration + " seconds")

  //Parallel Requests fired together
  val createUserRequest = scenario("Create User Scenario").exec(CreateUser.createUser)

  setUp(
    createUserRequest.inject(rampUsersPerSec(initialLoad) to (targetLoad) during (initialDuration.seconds), constantUsersPerSec(targetLoad) during (targetDuration.seconds)))
    .protocols(httpProtocol)
}
