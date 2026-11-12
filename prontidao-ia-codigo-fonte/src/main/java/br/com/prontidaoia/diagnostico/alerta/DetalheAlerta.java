package br.com.prontidaoia.diagnostico.alerta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Os valores que dispararam a regra, gravados em {@code alerta.detalhe}
 * (jsonb) no fechamento (decisão D13).
 *
 * @param regra   "A" ou "B"
 * @param ciclo   número do ciclo em que a regra foi avaliada
 * @param valores série de cada indicador usado, do ciclo mais antigo ao atual
 * @param frase   explicação em linguagem simples, exibida no Quadro 2 como está
 */
public record DetalheAlerta(String regra, int ciclo, Map<String, List<BigDecimal>> valores, String frase) {
}
