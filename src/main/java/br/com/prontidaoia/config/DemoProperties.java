package br.com.prontidaoia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do ambiente de demonstracao (secao 8 da especificacao).
 *
 * <p>So tem valores quando o perfil {@code demo} esta ativo.</p>
 *
 * @param ativo        liga a faixa "Ambiente de demonstracao" e o painel /demo
 * @param senhaPadrao  senha unica dos usuarios simulados, exibida em /login
 * @param semente      semente fixa do gerador; garante simulacao deterministica
 * @param restaurarAoIniciar com o banco sem ciclos, monta o cenario da secao 8.2
 *                     ao subir; os testes desligam para partir de um banco vazio
 */
@ConfigurationProperties(prefix = "prontidao.demo")
public record DemoProperties(boolean ativo, String senhaPadrao, long semente, boolean restaurarAoIniciar) {

    public DemoProperties {
        if (senhaPadrao == null) {
            senhaPadrao = "demo123";
        }
    }
}
