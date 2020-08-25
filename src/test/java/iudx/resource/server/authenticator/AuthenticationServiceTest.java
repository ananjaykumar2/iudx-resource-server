package iudx.resource.server.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@ExtendWith(VertxExtension.class)
public class AuthenticationServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceTest.class);
    private static final Properties properties = new Properties();
    private static Vertx vertxObj;
    private static AuthenticationService authenticationService;

    @BeforeAll
    @DisplayName("Initialize Vertx and deploy Auth Verticle")
    static void initialize(Vertx vertx, VertxTestContext testContext) {
        vertxObj = vertx;
        WebClient client = AuthenticationVerticle.createWebClient(vertxObj, properties, true);
        authenticationService = new AuthenticationServiceImpl(vertxObj, client);
        logger.info("Auth tests setup complete");
        testContext.completeNow();
        try {
            FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
            if (properties.isEmpty()) properties.load(configFile);
        } catch (IOException e) {
            logger.error("Could not load properties from config file", e);
        }
    }

    @Test
    @DisplayName("Testing setup")
    public void shouldSucceed(VertxTestContext testContext) {
        logger.info("Default test is passing");
        testContext.completeNow();
    }

    @Test
    @DisplayName("Test if webClient has been initialized properly")
    public void testWebClientSetup(VertxTestContext testContext) {
        WebClient client = AuthenticationVerticle.createWebClient(vertxObj, properties, true);
        String host = properties.getProperty(Constants.AUTH_SERVER_HOST);
        client.post(443, host, Constants.AUTH_CERTINFO_PATH).send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
                logger.error("Cert info call failed");
                testContext.failNow(httpResponseAsyncResult.cause());
                return;
            }
            logger.info("Cert info call to auth server succeeded");
            testContext.completeNow();
        });
    }

    @Test
    @DisplayName("Test if invalid token fails TIP")
    public void testInvalidToken(VertxTestContext testContext) {
        JsonObject request = new JsonObject()
                .put("ids", new JsonArray().add("testResource"));
        JsonObject authInfo = new JsonObject().put("token", "invalid")
                .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(0));
        authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
            if (asyncResult.failed()) {
                logger.error("Unexpected failure");
                testContext.failNow(asyncResult.cause());
                return;
            }
            JsonObject result = asyncResult.result();
            if (!result.getString("status").equals("error")) {
                testContext.failNow(new Throwable("Unexpected result for invalid token"));
                return;
            }
            logger.info("Invalid token TIP failed properly");
            logger.info(result.getString("message"));
            testContext.completeNow();
        });
    }

    @Test
    @DisplayName("Test the happy path without any caching")
    public void testHappyPath(VertxTestContext testContext) {
        JsonObject request = new JsonObject()
                .put("ids", new JsonArray()
                        .add(properties.getProperty("testResourceID")));
        JsonObject authInfo = new JsonObject()
                .put("token", properties.getProperty("testAuthToken"))
                .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(0));
        authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
            if (asyncResult.failed()) {
                logger.error("Unexpected failure");
                testContext.failNow(asyncResult.cause());
                return;
            }
            JsonObject result = asyncResult.result();
            if (result.getString("status").equals("error")) {
                testContext.failNow(new Throwable("Unexpected result"));
                return;
            }
            logger.info("Happy path test without caching success");
            testContext.completeNow();
        });
    }

    @Test
    @DisplayName("Test the happy path with TIP caching")
    public void testHappyPathTipCache(VertxTestContext testContext) {
        JsonObject request = new JsonObject()
                .put("ids", new JsonArray()
                        .add(properties.getProperty("testResourceID")));
        JsonObject authInfo = new JsonObject()
                .put("token", properties.getProperty("testAuthToken"))
                .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(0));
        authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
            if (asyncResult.failed()) {
                logger.error("Unexpected failure");
                testContext.failNow(asyncResult.cause());
                return;
            }
            JsonObject result = asyncResult.result();
            if (result.getString("status").equals("error")) {
                testContext.failNow(new Throwable("Unexpected result"));
                return;
            }

            JsonObject authInfo2 = new JsonObject()
                    .put("token", properties.getProperty("testAuthToken"))
                    .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
            authenticationService.tokenInterospect(request, authInfo2, asyncResult2 -> {
                if (asyncResult2.failed()) {
                    logger.error("Unexpected failure");
                    testContext.failNow(asyncResult2.cause());
                    return;
                }
                JsonObject result2 = asyncResult2.result();
                if (result2.getString("status").equals("error")) {
                    testContext.failNow(new Throwable("Unexpected result"));
                    return;
                }
                logger.info("Happy path test with caching success");
                testContext.completeNow();
            });
        });
    }

    @Test
    @DisplayName("Test closed endpoint with public token for expected failure")
    public void testClosedEndpointPublicToken(VertxTestContext testContext) {
        JsonObject request = new JsonObject()
                .put("ids", new JsonArray()
                        .add("datakaveri.org/1022f4c20542abd5087107c0b6de4cb3130c5b7b/example.com/res1"));
        JsonObject authInfo = new JsonObject()
                .put("token", "public")
                .put("apiEndpoint", Constants.CLOSED_ENDPOINTS.get(0));
        authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
            if (asyncResult.failed()) {
                logger.error("Unexpected failure");
                testContext.failNow(asyncResult.cause());
                return;
            }
            JsonObject result = asyncResult.result();
            if (!result.getString("status").equals("error")) {
                testContext.failNow(new Throwable("Unexpected result for public token and closed endpoint"));
                return;
            }
            logger.info("Public token for closed endpoint TIP failed properly");
            logger.info(result.getString("message"));
            testContext.completeNow();
        });
    }

    @Test
    @DisplayName("Test expired token for failure")
    public void testExpiredToken(VertxTestContext testContext) {
        JsonObject request = new JsonObject()
                .put("ids", new JsonArray()
                        .add("datakaveri.org/1022f4c20542abd5087107c0b6de4cb3130c5b7b/example.com/test-providers"));
        JsonObject authInfo = new JsonObject()
                .put("token", properties.getProperty("testExpiredAuthToken"))
                .put("apiEndpoint",  Constants.OPEN_ENDPOINTS.get(0));
        authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
            if (asyncResult.failed()) {
                logger.error("Unexpected failure");
                testContext.failNow(asyncResult.cause());
                return;
            }
            JsonObject result = asyncResult.result();
            if (!result.getString("status").equals("error")) {
                testContext.failNow(new Throwable("Unexpected success result for expired token"));
                return;
            }
            logger.info("Expired token TIP failed properly");
            logger.info(result.getString("message"));
            testContext.completeNow();
        });
    }

    @Test
    @DisplayName("Test CAT resource group ID API")
    public void testCatAPI(Vertx vertx, VertxTestContext testContext) {
        int catPort = Integer.parseInt(properties.getProperty("catServerPort"));
        String catHost = properties.getProperty("catServerHost");
        String catPath = Constants.CAT_RSG_PATH;
        String groupID = "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo";

        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true).setVerifyHost(false).setSsl(true);
        WebClient client = WebClient.create(vertx, options);
        client.get(catPort, catHost, catPath)
                .addQueryParam("property", "[id]")
                .addQueryParam("value", "[[" + groupID + "]]")
                .addQueryParam("filter", "[resourceAuthControlLevel]")
                .expect(ResponsePredicate.JSON).send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
                testContext.failNow(httpResponseAsyncResult.cause());
                return;
            }
            HttpResponse<Buffer> response = httpResponseAsyncResult.result();
            if (response.statusCode() != HttpStatus.SC_OK) {
                testContext.failNow(new Throwable(response.bodyAsString()));
                return;
            }
            JsonObject responseBody = response.bodyAsJsonObject();
            if (!responseBody.getString("status").equals("success")) {
                testContext.failNow(new Throwable(response.bodyAsString()));
                return;
            }
            String resourceACL = responseBody.getJsonArray("results")
                    .getJsonObject(0).getString("resourceAuthControlLevel");
            if (!resourceACL.equals("OPEN")) {
                testContext.failNow(new Throwable(response.bodyAsString()));
                return;
            }
            testContext.completeNow();
        });
    }


    @Test
    @DisplayName("Test CAT resource group invalid ID API")
    public void testCatAPIWithInvalidID(Vertx vertx, VertxTestContext testContext) {
        int catPort = Integer.parseInt(properties.getProperty("catServerPort"));
        String catHost = properties.getProperty("catServerHost");
        String catPath = Constants.CAT_RSG_PATH;
        String groupID = "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/invalid";

        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true).setVerifyHost(false).setSsl(true);
        WebClient client = WebClient.create(vertx, options);
        client.get(catPort, catHost, catPath)
                .addQueryParam("property", "[id]")
                .addQueryParam("value", "[[" + groupID + "]]")
                .addQueryParam("filter", "[resourceAuthControlLevel]")
                .expect(ResponsePredicate.JSON).send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
                testContext.failNow(httpResponseAsyncResult.cause());
                return;
            }
            HttpResponse<Buffer> response = httpResponseAsyncResult.result();
            if (response.statusCode() != HttpStatus.SC_OK) {
                testContext.failNow(new Throwable(response.bodyAsString()));
                return;
            }
            JsonObject responseBody = response.bodyAsJsonObject();
            if (!responseBody.getString("status").equals("success")) {
                testContext.failNow(new Throwable(response.bodyAsString()));
                return;
            }

            if (!responseBody.containsKey("resourceAuthControlLevel")) {
                testContext.failNow(new Throwable(response.bodyAsString()));
                return;
            }
            testContext.completeNow();
        });
    }

}
