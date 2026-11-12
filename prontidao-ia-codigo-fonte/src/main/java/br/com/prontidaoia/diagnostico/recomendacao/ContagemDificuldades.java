package br.com.prontidaoia.diagnostico.recomendacao;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Seção 5.3, alerta A: conta as dificuldades marcadas nos check-ins do time
 * no ciclo e pega as <b>duas mais marcadas</b>, com empate resolvido pela
 * ordem do catálogo (a ordem de id, decisão D10).
 *
 * <p>Uma dificuldade que ninguém marcou não vira recomendação (decisão D38).
 * No cenário do Atendimento, ciclo 4: 5, 3, 2, 1 e 0 marcações; saem
 * "receio de substituição" (5 de 7) e "não sei quando confiar" (3 de 7).</p>
 */
public final class ContagemDificuldades {

    private ContagemDificuldades() {
    }

    /** Uma dificuldade e quantas vezes foi marcada. */
    public record Marcacao(long dificuldadeId, long vezes) {
    }

    /**
     * @param vezesPorDificuldade marcações por id de dificuldade
     * @param quantas             quantas dificuldades devolver (2 na seção 5.3)
     */
    public static List<Marcacao> maisMarcadas(Map<Long, Long> vezesPorDificuldade, int quantas) {
        return vezesPorDificuldade.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new Marcacao(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Marcacao::vezes).reversed()
                        .thenComparingLong(Marcacao::dificuldadeId))
                .limit(quantas)
                .toList();
    }
}
