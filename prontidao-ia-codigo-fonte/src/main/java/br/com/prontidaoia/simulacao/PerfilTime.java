package br.com.prontidaoia.simulacao;

import java.util.Map;

/**
 * As metas da seção 8.4, transcritas como estão, e as contagens de
 * dificuldade de cada time por ciclo.
 *
 * <p>Só as dificuldades do Atendimento no ciclo 4 vêm da especificação; as
 * demais foram escolhidas para parecerem plausíveis e não afetam nenhuma
 * regra (decisão D43).</p>
 */
public final class PerfilTime {

    private PerfilTime() {
    }

    /**
     * Metas de um time num ciclo.
     *
     * @param tecnico      % de acerto nas atividades
     * @param confianca    confiança (0 a 100)
     * @param resistencia  resistência (0 a 100)
     * @param engajamento  pendências concluídas (%)
     * @param verificacao  % de acerto em VERIFICACAO
     * @param tempoMin     tempo médio por atividade, em minutos
     * @param acertoPorArea acerto fixo por área; só no ciclo 4, da tabela de erro por área
     * @param dificuldades quantas pessoas marcam cada dificuldade (código do catálogo)
     */
    public record Metas(
            double tecnico,
            double confianca,
            double resistencia,
            double engajamento,
            double verificacao,
            double tempoMin,
            Map<String, Double> acertoPorArea,
            Map<String, Integer> dificuldades) {
    }

    private static final String R = "RECEIO_SUBSTITUICAO";
    private static final String C = "NAO_SEI_QUANDO_CONFIAR";
    private static final String T = "FALTA_TEMPO_SUPORTE";
    private static final String E = "NAO_SE_ENCAIXA";
    private static final String L = "LIDERANCA_NAO_USA";

    private static Map<String, Integer> dif(int r, int c, int t, int e, int l) {
        return Map.of(R, r, C, c, T, t, E, e, L, l);
    }

    /** Acerto por área a partir da tabela "Erro por área no ciclo 4": acerto = 100 − erro. */
    private static Map<String, Double> erroC4(double formulacao, double verificacao, double dados, double aplicacao) {
        return Map.of("FORMULACAO", 100 - formulacao, "VERIFICACAO", 100 - verificacao,
                "DADOS", 100 - dados, "APLICACAO", 100 - aplicacao);
    }

    //                                          técnico conf resist engaj verif tempo
    private static final Metas[] ATENDIMENTO = {
            new Metas(55, 63, 44, 96, 60, 14.0, null, dif(1, 2, 1, 0, 1)),
            new Metas(61, 59, 47, 92, 63, 12.8, null, dif(2, 2, 1, 1, 0)),
            new Metas(67, 53, 53, 92, 65, 11.6, null, dif(3, 3, 2, 1, 0)),
            // Ciclo 4: verificação 67 (tabela de erro por área, decisão D02).
            new Metas(72, 47, 63, 79, 67, 10.9, erroC4(23, 33, 25, 33), dif(5, 3, 2, 1, 0)),
            new Metas(73, 50, 59, 83, 73, 10.6, null, dif(3, 2, 1, 1, 0)),
            new Metas(75, 56, 53, 92, 78, 10.2, null, dif(1, 1, 1, 0, 0)),
    };

    private static final Metas[] FINANCEIRO = {
            new Metas(58, 34, 41, 83, 53, 15.5, null, dif(1, 3, 4, 2, 2)),
            new Metas(61, 41, 38, 88, 55, 14.6, null, dif(1, 3, 3, 2, 1)),
            new Metas(65, 47, 34, 88, 60, 13.8, null, dif(1, 2, 3, 1, 1)),
            new Metas(69, 53, 31, 92, 65, 13.0, erroC4(30, 35, 23, 38), dif(0, 2, 2, 1, 1)),
            new Metas(72, 59, 28, 92, 68, 12.4, null, dif(0, 1, 2, 1, 0)),
            new Metas(75, 63, 28, 96, 73, 11.9, null, dif(0, 1, 1, 0, 0)),
    };

    private static final Metas[] MARKETING = {
            new Metas(64, 78, 16, 100, 38, 9.8, null, dif(0, 0, 1, 1, 0)),
            new Metas(65, 81, 16, 96, 40, 9.6, null, dif(0, 0, 1, 0, 0)),
            new Metas(66, 84, 13, 100, 40, 9.5, null, dif(0, 0, 1, 0, 0)),
            new Metas(67, 84, 13, 96, 43, 9.4, erroC4(18, 58, 33, 25), dif(0, 0, 1, 0, 0)),
            new Metas(68, 88, 13, 100, 43, 9.4, null, dif(0, 0, 0, 1, 0)),
            new Metas(69, 88, 9, 100, 45, 9.3, null, dif(0, 0, 0, 0, 0)),
    };

    public static Metas de(String time, int ciclo) {
        Metas[] tabela = switch (time) {
            case PessoasDemo.TIME_ATENDIMENTO -> ATENDIMENTO;
            case PessoasDemo.TIME_FINANCEIRO -> FINANCEIRO;
            case PessoasDemo.TIME_MARKETING -> MARKETING;
            default -> throw new IllegalArgumentException("Time fora do cenário: " + time);
        };
        if (ciclo < 1 || ciclo > tabela.length) {
            throw new IllegalArgumentException("Ciclo fora do cenário: " + ciclo);
        }
        return tabela[ciclo - 1];
    }
}
