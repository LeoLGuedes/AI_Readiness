package br.com.prontidaoia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Monitoramento da prontidao para IA generativa.
 *
 * <p>Artefato de Design Science Research: acompanha, ciclo a ciclo, a prontidao
 * de times para o uso de IA generativa, cruzando desempenho (atividades) e
 * percepcao (check-in).</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ProntidaoIaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProntidaoIaApplication.class, args);
    }
}
