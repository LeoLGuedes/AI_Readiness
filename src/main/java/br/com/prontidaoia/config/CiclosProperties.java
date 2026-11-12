package br.com.prontidaoia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do acompanhamento por ciclos (secao 5.5 da especificacao).
 *
 * @param total        quantos ciclos o acompanhamento tem; alimenta o texto
 *                     "Ciclo 4 de 6" e a decisao de abrir ou nao o proximo
 * @param diasPorCiclo duracao de cada ciclo, usada ao abrir o proximo
 */
@ConfigurationProperties(prefix = "prontidao.ciclos")
public record CiclosProperties(int total, int diasPorCiclo) {
}
