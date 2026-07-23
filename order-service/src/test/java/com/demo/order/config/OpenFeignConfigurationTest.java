package com.demo.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class OpenFeignConfigurationTest {

    @Test
    void productClientHasBoundedTimeouts() throws Exception {
        var propertySource = new YamlPropertySourceLoader()
                .load("order-service", new ClassPathResource("application.yml"))
                .getFirst();

        assertThat(propertySource.getProperty(
                "spring.cloud.openfeign.client.config.product-service.connect-timeout"))
                .isEqualTo(1000);
        assertThat(propertySource.getProperty(
                "spring.cloud.openfeign.client.config.product-service.read-timeout"))
                .isEqualTo(1500);
    }
}
