package dev.haguel.expirenza_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExpirenzaCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpirenzaCrawlerApplication.class, args);
    }
}
