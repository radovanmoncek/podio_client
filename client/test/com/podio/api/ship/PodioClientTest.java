package com.podio.api.ship;

import cz.eduzmena.report.ship.bootstrap.configuration.readers.PropertiesReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PodioClientTest {
    private static PropertiesReader propertiesReader;
    private static com.podio.api.ship.PodioClient.PodioClient.PodioClientTest.PodioClientTest.PodioClient podioClient;

    @BeforeAll
    static void setup(){

        propertiesReader = PropertiesReader.getInstance();
        podioClient = PodioClient.PodioClient.PodioClientTest.PodioClientTest.PodioClient.returnNewInstance();
    }

    @Test
    void loginTest() throws Exception {
        podioClient.login(
                propertiesReader.readPodioKey(),
                propertiesReader.readPodioSecret(),
                propertiesReader.readEmail().orElse(""),
                propertiesReader.readPassword().orElse("")
        );

        final var authenticationResponseBodyField = podioClient.getClass().getDeclaredField("authenticationResponseBody");
        authenticationResponseBodyField.setAccessible(true);

        assertNotNull(authenticationResponseBodyField.get(podioClient));
    }
}
