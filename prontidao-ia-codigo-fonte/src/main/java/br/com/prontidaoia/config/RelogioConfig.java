package br.com.prontidaoia.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio único da aplicação. Os serviços pedem a hora a ele, e não a
 * {@code Instant.now()}, para que os testes possam controlar o tempo.
 */
@Configuration
public class RelogioConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
