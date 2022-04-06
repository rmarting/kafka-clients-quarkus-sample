package io.jromanmartin.kafka.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ProducerControllerTest {

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/producer")
          .then()
             .statusCode(200)
             .body(is("Hello Producer!"));
    }

}
