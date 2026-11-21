package com.yanki.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the wallet-service microservice.
 *
 * <p>Part of the Yanki banking platform. The service follows the
 * database-per-service pattern: it owns and accesses only its own MongoDB
 * database and never reads nor writes the data owned by another
 * microservice. Cross-service collaboration is performed through Kafka
 * events and never through direct REST calls.</p>
 */
@SpringBootApplication
public class WalletServiceApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command line arguments forwarded to the Spring context
     */
    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}