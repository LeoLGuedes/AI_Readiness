package br.com.prontidaoia.simulacao;

import java.util.List;
import java.util.Map;

/**
 * O roteiro da Carla (seção 8.3).
 *
 * <ul>
 *   <li><b>Ciclo 3 (histórico):</b> acerto pessoal de 65%, ou 13 de 20. É
 *       gerado junto com o cenário.</li>
 *   <li><b>Ciclo 4 (ao vivo):</b> check-in com confiança 2 e receio 4,
 *       marcando "receio de substituição" e "não sei quando posso confiar";
 *       4A com 8 de 10 em cerca de 11 minutos; 4B com 7 de 10 em cerca de 10
 *       minutos. O gerador <b>reserva</b> essas pendências: não as preenche,
 *       mas desconta os valores do roteiro das metas do time.</li>
 * </ul>
 */
public final class RoteiroCarla {

    private RoteiroCarla() {
    }

    /** Ciclo em que a Carla responde ao vivo. */
    public static final int CICLO_AO_VIVO = 4;

    public static final int CONFIANCA = 2;
    public static final int RECEIO = 4;
    public static final List<String> DIFICULDADES = List.of("RECEIO_SUBSTITUICAO", "NAO_SEI_QUANDO_CONFIAR");

    /**
     * Uma atividade do roteiro: acertos por área e duração.
     *
     * @param slot     "A" ou "B", a posição da atividade no escopo
     * @param acertos  acertos por código de área (5 questões por área)
     * @param segundos duração da tentativa
     */
    public record AtividadeFixa(String slot, Map<String, Integer> acertos, int segundos) {

        public int totalDeAcertos() {
            return acertos.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /** Ciclo 4: 4A com 8 de 10 (≈ 11 min) e 4B com 7 de 10 (≈ 10 min). */
    public static final List<AtividadeFixa> ATIVIDADES_CICLO_4 = List.of(
            new AtividadeFixa("A", Map.of("FORMULACAO", 4, "VERIFICACAO", 4), 11 * 60 + 5),
            new AtividadeFixa("B", Map.of("DADOS", 4, "APLICACAO", 3), 10 * 60 - 10));

    /** Ciclo 3: 13 de 20, ou 65%, o "acerto pessoal no ciclo 3" da seção 8.3. */
    public static final List<AtividadeFixa> ATIVIDADES_CICLO_3 = List.of(
            new AtividadeFixa("A", Map.of("FORMULACAO", 3, "VERIFICACAO", 3), 12 * 60 + 20),
            new AtividadeFixa("B", Map.of("DADOS", 4, "APLICACAO", 3), 11 * 60 + 40));

    /** Atividades que o gerador cria para a Carla com resultado fixo, por ciclo. */
    public static List<AtividadeFixa> fixasGeradas(int ciclo) {
        return ciclo == 3 ? ATIVIDADES_CICLO_3 : List.of();
    }
}
