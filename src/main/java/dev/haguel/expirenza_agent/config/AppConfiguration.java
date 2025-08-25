package dev.haguel.expirenza_agent.config;

import dev.haguel.expirenza_agent.main.impl.ExpirenzaMenuURLBuffer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingDeque;

@Configuration
public class AppConfiguration {
    @Bean
    public ExpirenzaMenuURLBuffer getExpirenzaMenuURLBuffer() {
        return new ExpirenzaMenuURLBuffer(new LinkedBlockingDeque<>());
    }
}
