package br.com.prontidaoia.diagnostico.alerta;

import java.math.BigDecimal;

/**
 * Os valores de um time num ciclo que as regras de alerta leem.
 *
 * <p>Técnico, confiança e engajamento vêm da fotografia gravada; o acerto em
 * verificação é calculado das respostas, que não mudam depois do fechamento.
 * Confiança nula significa amostra insuficiente.</p>
 */
public record PontoDoCiclo(
        int ciclo,
        BigDecimal tecnico,
        BigDecimal confianca,
        BigDecimal engajamento,
        BigDecimal acertoVerificacao) {

    public boolean temAmostra() {
        return confianca != null;
    }
}
