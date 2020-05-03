package com.exness.api.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;

import java.util.concurrent.TimeUnit;

import com.exness.core.shared.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;


public class UploadsServiceTest {
	private static final Logger LOG = LoggerFactory.getLogger(UploadsServiceTest.class);

    private static final String PING = "/ping/";
    private static final String AUTHORIZE = "/authorize/";
    private static final String SAVE_DATA = "/api/save_data/";

    private static final String USER = Config.getProperty("api.username");
    private static final String PASSWORD = Config.getProperty("api.password");

    private RequestSpecBuilder requestSpecBuilder;
    private RequestSpecification requestJSONSpec;

    private String token;
    private Long tokenExpirationDate;

    @BeforeSuite
    public void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        requestSpecBuilder = new RequestSpecBuilder()
                .setBaseUri(Config.getProperty("api.base_url"))
                .setPort(5000)
                .setAccept(ContentType.JSON);
        requestJSONSpec = requestSpecBuilder.setContentType(ContentType.JSON).build();
    }

    @Test(description = "Ping API success.")
    public void pingSuccess() {
        given()
                .spec(requestJSONSpec)
                .when()
                .get(PING)
                .then()
                .statusCode(200);
    }

    @Test(description = "Get token with valid user/password.")
    public void authorizeSuccess() {
        getToken(false);
    }

    @Test(description = "Failed get token with incorrect user/password.")
    public void authorizeFailIncorrectCreds() {
        given()
                .spec(requestJSONSpec)
                .param("username", "dummy")
                .param("password", "dummy")
                .when()
                .post(AUTHORIZE)
                .then()
                .statusCode(not(equalTo(200)));
    }

    @Test(description = "Failed get token because user/password are not specified.")
    public void authorizeFailEmptyCreds() {
        given()
                .spec(requestJSONSpec)
                .when()
                .post(AUTHORIZE)
                .then()
                .statusCode(not(equalTo(200)));
    }

    @Test(description = "Upload JSON data success.")
    public void saveDataJSONSuccess() {
        given()
                .spec(requestJSONSpec)
                .auth()
                .oauth2(getToken(false))
                .when()
                .body("{\"payload\": \"DATA\"}")
                .post(SAVE_DATA)
                .then()
                .statusCode(200)
                .body("$", hasKey("status"))
                .body("status", is(oneOf("OK", "ERROR")))
                .body("id", response -> {
                    if ("OK" .equals(response.body().path("status"))) {
                        return not(blankOrNullString());
                    } else {
                        return is(nullValue());
                    }
                })
                .body("error", response -> {
                    if ("ERROR" .equals(response.body().path("status"))) {
                        return not(blankOrNullString());
                    } else {
                        return is(nullValue());
                    }
                });
    }

    @Test(description = "Upload JSON data fail: incorrect JSON schema.")
    public void saveDataJSONFailIncorrectData() {
        given()
                .spec(requestJSONSpec)
                .auth()
                .oauth2(getToken(false))
                .when()
                .body("{\"some_key\": \"DATA\"}")
                .post(SAVE_DATA)
                .then()
                .statusCode(400);
    }

    @Test(description = "Upload JSON data fail: no JSON data.")
    public void saveDataJSONFailWithoutData() {
        given()
                .spec(requestJSONSpec)
                .auth()
                .oauth2(getToken(false))
                .when()
                .post(SAVE_DATA)
                .then()
                .statusCode(400);
    }

    @Test(description = "Upload JSON data fail: unauthorized; emty token.")
    public void saveDataUnauthorizedEmptyToken() {
        given()
                .spec(requestJSONSpec)
                .when()
                .body("{\"payload\": \"DATA\"}")
                .post(SAVE_DATA)
                .then()
                .statusCode(403);
    }

    @Test(description = "Upload JSON data fail: unauthorized; incorrect token.")
    public void saveDataUnauthorizedIncorrectToken() {
        given()
                .spec(requestJSONSpec)
                .auth()
                .oauth2("abcd")
                .when()
                .body("{\"payload\": \"DATA\"}")
                .post(SAVE_DATA)
                .then()
                .statusCode(403);
    }

    @Test(description = "Upload JSON data fail: unauthorized; expired token.")
    public void saveDataUnauthorizedExpiredToken() {
        given()
                .spec(requestJSONSpec)
                .auth()
                .oauth2(getToken(true))
                .when()
                .body("{\"payload\": \"DATA\"}")
                .post(SAVE_DATA)
                .then()
                .statusCode(403);
    }

    @Test(description = "Upload form data success.")
    public void saveDataURLEncodedSuccess() {
        given()
                .spec(requestSpecBuilder.setContentType(ContentType.URLENC).build())
                .auth()
                .oauth2(getToken(false))
                .when()
                .formParam("payload", "DATA")
                .post(SAVE_DATA)
                .then()
                .statusCode(200)
                .body("$", hasKey("status"))
                .body("status", is(oneOf("OK", "ERROR")))
                .body("id", response -> {
                    if ("OK".equals(response.body().path("status"))) {
                        return not(blankOrNullString());
                    } else {
                        return is(nullValue());
                    }
                })
                .body("error", response -> {
                    if ("ERROR".equals(response.body().path("status"))) {
                        return not(blankOrNullString());
                    } else {
                        return is(nullValue());
                    }
                });
    }

    @Test(description = "Upload form data fail: incorrect parameter.")
    public void saveDataURLEncodedFailIncorrectData() {
        given()
                .spec(requestSpecBuilder.setContentType(ContentType.URLENC).build())
                .auth()
                .oauth2(getToken(false))
                .when()
                .formParam("some_param", "DATA")
                .post(SAVE_DATA)
                .then()
                .statusCode(400);
    }

    @Test(description = "Upload form data fail: empty data.")
    public void saveDataURLEncodedFailEmptyData() {
        given()
                .spec(requestSpecBuilder.setContentType(ContentType.URLENC).build())
                .auth()
                .oauth2(getToken(false))
                .when()
                .post(SAVE_DATA)
                .then()
                .statusCode(400);
    }

    private String getToken(boolean waitForExpiration) {
        if (token == null) {
            token = given()
                    .spec(requestJSONSpec)
                    .param("username", USER)
                    .param("password", PASSWORD)
                    .when()
                    .post(AUTHORIZE)
                    .then()
                    .statusCode(200)
                    .body("$", hasKey("token"))
                    .body("token", not(blankOrNullString()))
                    .extract()
                    .path("token");
            tokenExpirationDate = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        }


        if (waitForExpiration && System.currentTimeMillis() < tokenExpirationDate) {
            String expiredToken;
            try {
				LOG.info("Waiting for token expiration...");

                TimeUnit.MILLISECONDS.sleep((tokenExpirationDate - System.currentTimeMillis()) + 100);
                expiredToken = new String(token);
            } catch (Exception e) {
                throw new RuntimeException("Error wait for token expiration", e);
            } finally {
                token = null;
                tokenExpirationDate = null;
            }

            return expiredToken;
        }

        return token;
    }
}
