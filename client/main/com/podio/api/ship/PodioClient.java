package com.podio.api.ship;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class server as a wrapper for the Podio API HTTP client requests.
 */
public final class PodioClient {
    private static final Logger logger = Logger.getLogger(PodioClient.class.getName());
    private static final String PODIO_API_BASE_URI = "https://api.podio.com/";
    private static final String OAUTH_URI = "https://api.podio.com/oauth/token/v2";
    public static final String APP_ENDPOINT = "app/";
    public static final String ITEM_ENDPOINT = "item/";
    public static final String ORG_ENDPOINT = "org/";
    public static final String SPACE_ENDPOINT = "space/";
    public static final String USER_ENDPOINT = "user/";
    public static final String REFERENCE_ENDPOINT = "reference/";
    private static PodioClient instance;
    private final Gson gson;
    private final HttpClient httpClient;
    private final Timer tokenRefreshTimer;
    private long callCount = 0;
    private JsonObject authenticationResponseBody;
    private TimerTask tokenRefreshTask;

    private PodioClient() {

        gson = new Gson();
        httpClient = HttpClient.newHttpClient();
        tokenRefreshTimer = new Timer();
        tokenRefreshTask = new TimerTask() {
            @Override
            public void run() {
            }
        };
    }

    public static PodioClient returnNewInstance() {

        return Objects.requireNonNullElse(instance, instance = new PodioClient());
    }

    public void login(final String clientID, final String clientSecret, final String email, final String password) throws Exception {

        try {

            final var loginRequestBody = new JsonObject();

            loginRequestBody.addProperty("grant_type", "password");
            loginRequestBody.addProperty("username", email);
            loginRequestBody.addProperty("password", password);
            loginRequestBody.addProperty("client_id", clientID);
            loginRequestBody.addProperty("redirect_uri", "");
            loginRequestBody.addProperty("client_secret", clientSecret);

            final var loginPOSTRequest = HttpRequest
                    .newBuilder()
                    .uri(new URI(OAUTH_URI))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequestBody.toString()))
                    .build();

            logger.info(loginPOSTRequest.toString());

            final var loginPOSTResponse = httpClient.send(loginPOSTRequest, HttpResponse.BodyHandlers.ofString());

            if (loginPOSTResponse.statusCode() >= 400)
                throw new Exception("Podio API authentication failed");

            authenticationResponseBody = gson.fromJson(loginPOSTResponse.body(), JsonObject.class);

            logger.info(authenticationResponseBody.toString());

            tokenRefreshTimer.schedule(tokenRefreshTask = new TimerTask() {

                        public void run() {

                            try {

                                final var refreshTokenRequest = new JsonObject();

                                refreshTokenRequest.addProperty("grant_type", "refresh_token");
                                refreshTokenRequest.addProperty("refresh_token", authenticationResponseBody.get("refresh_token").toString());
                                refreshTokenRequest.addProperty("client_id", clientID);
                                refreshTokenRequest.addProperty("client_secret", clientSecret);

                                final var loginPOSTRefreshTokenRequest = HttpRequest
                                        .newBuilder()
                                        .uri(new URI(OAUTH_URI))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(refreshTokenRequest.getAsString()))
                                        .build();

                                httpClient.send(loginPOSTRefreshTokenRequest, HttpResponse.BodyHandlers.ofString());
                            } catch (URISyntaxException | InterruptedException | IOException e) {

                                logger.throwing(getClass().getName(), "login", e);
                            }
                        }
                    },
                    authenticationResponseBody
                            .get("expires_in")
                            .getAsLong() * 1000
            );
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.throwing(getClass().getName(), "login", e);
        }
    }

    public <T> Optional<T> GET(final String request, final TypeToken<T> responseType) {

        try {

            logger.log(Level.INFO, "{0} GET", PODIO_API_BASE_URI + request);

            final var httpResponse = httpClient
                    .send(
                            HttpRequest
                                    .newBuilder()
                                    .uri(new URI(PODIO_API_BASE_URI + request))
                                    .header("Authorization", "OAuth2 " + authenticationResponseBody.get("access_token").getAsString())
                                    .GET()
                                    .build()
                            , HttpResponse.BodyHandlers.ofString()
                    );

            logger.log(Level.FINEST, "Current call count: {0}", ++callCount);

            if (httpResponse.statusCode() >= 400) {

                logger.log(Level.SEVERE, "Response code above 400 {0}", httpResponse);

                if (httpResponse.statusCode() == 420)
                    logger.severe("Rate limit reached");

                return Optional.empty();
            }

            TimeUnit.MILLISECONDS.sleep(500);

            final var deserializedResponse = gson.fromJson(httpResponse.body(), responseType);

            return Optional.of(deserializedResponse);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            logger.throwing(getClass().getName(), "GET", e);

            return Optional.empty();
        }
    }

    private static String filterComma(String string) {//TODO: String sanitizer method (general filter of html tags etc.)

        return string.replace(",", " ");
    }

    public <T> Optional<T> POST(final String request, final JsonObject body, final TypeToken<T> returnType) {
        try {
            logger.log(Level.INFO, "{0} POST", PODIO_API_BASE_URI + request);

            final var httpResponse = httpClient.send(HttpRequest
                    .newBuilder()
                    .uri(URI.create(PODIO_API_BASE_URI + request))
                    .header("Authorization", "OAuth2 " + authenticationResponseBody.get("access_token").getAsString())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build(), HttpResponse.BodyHandlers.ofString());

            logger.log(Level.FINEST, "Current call count: {0}", ++callCount);

            if (httpResponse.statusCode() >= 400) {
                logger.log(Level.FINEST, "Response code above 400 {0}", httpResponse);

                if (httpResponse.statusCode() == 420)
                    logger.severe("Rate limit reached");

                return Optional.empty();
            }

            TimeUnit.MILLISECONDS.sleep(500);

            final var deserializedResponse = gson.fromJson(httpResponse.body(), returnType);

            return Optional.of(deserializedResponse);
        } catch (IOException | InterruptedException e) {

            logger.throwing(getClass().getName(), "POST", e);

            return Optional.empty();
        }
    }

    public void close() {
//        todo: httpClient.close(); & auto closable
        tokenRefreshTask.cancel(); //todo:
        tokenRefreshTimer.cancel();
    }
}
