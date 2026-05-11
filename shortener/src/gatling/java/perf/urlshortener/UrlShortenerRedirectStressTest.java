package perf.urlshortener;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.core.CoreDsl.stressPeakUsers;
import static io.gatling.javaapi.http.HttpDsl.header;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

///
/// Dedicated redirect-only stress test.
/// Seeds 100 short codes, then hits only GET /:shortCode with aggressive load.
/// Designed to measure redirect performance in isolation.
///
public class UrlShortenerRedirectStressTest extends Simulation {

    private static final List<String> SHORT_CODES = new CopyOnWriteArrayList<>();
    private static final int SEED_COUNT = 100;

    private final String keycloakUrl = System.getProperty("keycloakUrl", "http://localhost:8180");

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling-RedirectStress/1.0");

    private final ScenarioBuilder seedScenario = scenario("Seed Short Codes")
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
        .repeat(SEED_COUNT, "iteration")
        .on(exec(
            http("POST /api/v1/urls")
                .post("/api/v1/urls")
                .header("Authorization", "Bearer #{accessToken}")
                .body(StringBody(_ -> createShortCodeRequest()))
                .asJson()
                .check(status().is(201))
                .check(jsonPath("$.shortCode").saveAs("shortCode")))
            .exec(session -> {
                SHORT_CODES.add(session.getString("shortCode"));
                return session;
            })
        );

    private final ScenarioBuilder redirectScenario = scenario("Redirect Stress")
        .feed(() -> Stream.generate(() -> Map.<String, Object>of("shortCode",
                SHORT_CODES.get(ThreadLocalRandom.current().nextInt(SHORT_CODES.size()))))
            .iterator())
        .exec(http("GET /:shortCode (redirect)")
            .get("/#{shortCode}")
            .disableFollowRedirect()
            .check(status().is(302))
            .check(header("Location").exists()));

    {
        setUp(seedScenario.injectOpen(atOnceUsers(1))
            .andThen(redirectScenario.injectOpen(
                rampUsersPerSec(10).to(2000).during(Duration.ofMinutes(2)),
                stressPeakUsers(40000).during(Duration.ofMinutes(1))
            )))
            .protocols(httpProtocol)
            .assertions(
                global().successfulRequests().percent().is(100.0),
                global().responseTime().percentile(95.0).lt(100),
                global().responseTime().percentile(99.0).lt(200)
            );
    }

    private static String createShortCodeRequest() {
        return """
            {
              "url":"https://example.com/stress/resource/%s",
              "ttlSeconds":86400
            }""".formatted(UUID.randomUUID());
    }

}
