package br.com.prontidaoia.diagnostico.indicador;

/** Uma resposta de tentativa finalizada: a área da questão e se acertou. */
public record RespostaAvaliada(String area, boolean correta) {
}
