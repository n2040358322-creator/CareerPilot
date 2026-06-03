package com.careerpilot;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CareerPilotApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(CareerPilotApplication.class, args);
    }

    private static void loadDotenv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
        setSpringProperty(dotenv, "SPRING_PROFILES_ACTIVE", "spring.profiles.active");
        setSpringProperty(dotenv, "MYSQL_URL", "spring.datasource.url");
        setSpringProperty(dotenv, "MYSQL_USERNAME", "spring.datasource.username");
        setSpringProperty(dotenv, "MYSQL_PASSWORD", "spring.datasource.password");
        setSpringProperty(dotenv, "JPA_DDL_AUTO", "spring.jpa.hibernate.ddl-auto");
    }

    private static void setSpringProperty(Dotenv dotenv, String envName, String propertyName) {
        if (System.getProperty(propertyName) != null || System.getenv(propertyName) != null) {
            return;
        }
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = dotenv.get(envName);
        }
        if (value != null && !value.isBlank()) {
            System.setProperty(propertyName, value);
        }
    }
}
