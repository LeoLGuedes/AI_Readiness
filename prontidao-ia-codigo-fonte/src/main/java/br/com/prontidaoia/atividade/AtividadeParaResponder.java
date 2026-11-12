package br.com.prontidaoia.atividade;

import java.time.Instant;
import java.util.List;

/** O que a tela de execução mostra: a atividade, as questões e o início do cronômetro. */
public record AtividadeParaResponder(
        Long liberadaId,
        String codigo,
        String titulo,
        String descricao,
        Instant iniciadaEm,
        List<QuestaoParaResponder> questoes) {

    /** Uma questão sem o gabarito: a tela de execução nunca recebe a resposta certa. */
    public record QuestaoParaResponder(Long id, int ordem, String area, String enunciado, List<String> alternativas) {
    }

    /** Início em milissegundos, para o cronômetro em JavaScript continuar após recarregar. */
    public long iniciadaEmMillis() {
        return iniciadaEm.toEpochMilli();
    }
}
