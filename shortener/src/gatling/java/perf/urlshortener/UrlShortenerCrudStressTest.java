package perf.urlshortener;

import java.time.Duration;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.csv;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.core.CoreDsl.stressPeakUsers;
import static io.gatling.javaapi.http.HttpDsl.header;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

///
/// Stress test for the URL Shortener.
/// Simulates a full CRUD cycle (create, read, redirect, delete)
/// with ramping and peak load injection profiles.
///
public class UrlShortenerCrudStressTest extends Simulation {

    private static String token;

    private final String keycloakUrl = System.getProperty("keycloakUrl", "http://localhost:8180");

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling-StressTest/1.0");

    private final FeederBuilder<String> urlFeeder = csv("data/urls.csv").circular();

    private final ScenarioBuilder authScenario = scenario("Authentication")
        .exec(http("POST Keycloak token")
            .post(keycloakUrl + "/realms/shortener/protocol/openid-connect/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .formParam("grant_type", "password")
            .formParam("client_id", "shortener-client")
            .formParam("username", "testuser")
            .formParam("password", "test")
            .check(status().is(200))
            .check(jsonPath("$.access_token").exists())
            .check(jsonPath("$.access_token").saveAs("accessToken")))
        .exec(session -> {
            token = session.getString("accessToken");
            return session;
        });

    private final ScenarioBuilder crudCycle = scenario("Full CRUD Cycle")
        .feed(urlFeeder)
        .exec(http("POST /api/v1/urls")
            .post("/api/v1/urls")
            .header("Authorization", _ -> "Bearer " + token)
            .body(StringBody("{\"url\":\"#{longUrl}\",\"ttlSeconds\":#{ttlSeconds}}"))
            .asJson()
            .check(status().is(201))
            .check(jsonPath("$.shortCode").exists())
            .check(jsonPath("$.shortCode").saveAs("shortCode")))
        .pause(1, 2)
        .exec(http("GET /api/v1/urls/:shortCode")
            .get("/api/v1/urls/#{shortCode}")
            .header("Authorization", _ -> "Bearer " + token)
            .check(status().is(200))
            .check(jsonPath("$.originalUrl").exists())
            .check(jsonPath("$.shortCode").isEL("#{shortCode}")))
        .pause(1, 3)
        .exec(http("GET /:shortCode (redirect)")
            .get("/#{shortCode}")
            .disableFollowRedirect()
            .check(status().is(302))
            .check(header("Location").exists()))
        .pause(1, 2)
        .exec(http("DELETE /api/v1/urls/:shortCode")
            .delete("/api/v1/urls/#{shortCode}")
            .header("Authorization", _ -> "Bearer " + token)
            .check(status().is(204)));

    {
        setUp(authScenario.injectOpen(atOnceUsers(1))
            .andThen(crudCycle.injectOpen(
                rampUsersPerSec(10).to(400).during(Duration.ofMinutes(3)),
                stressPeakUsers(8000).during(Duration.ofMinutes(1))
            )))
            .protocols(httpProtocol)
            .assertions(
                global().successfulRequests().percent().is(100.0),
                global().responseTime().percentile(95.0).lt(100),
                global().responseTime().percentile(99.0).lt(200)
            );
    }

}
