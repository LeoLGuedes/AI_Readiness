package br.com.prontidaoia;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes que precisam de banco.
 *
 * <p>O contêiner é um só para toda a suíte (padrão "singleton container"):
 * subir um PostgreSQL por classe de teste deixaria a execução lenta demais
 * para o ritmo de fatias verticais.</p>
 */
public abstract class TesteComBanco {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("prontidao")
            .withUsername("prontidao")
            .withPassword("prontidao");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void apontarParaOContainer(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        // Nos testes, só o próprio teste fecha ciclo.
        registro.add("prontidao.ciclos.fechamento-automatico", () -> "false");
        // Os testes partem do ciclo 1 vazio; quem precisa do cenário o restaura.
        registro.add("prontidao.demo.restaurar-ao-iniciar", () -> "false");
    }
}
