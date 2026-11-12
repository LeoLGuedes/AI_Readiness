package br.com.prontidaoia.diagnostico.alerta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A tabela de metas da seção 8.4, transcrita como séries de pontos. Os testes
 * de regra usam exatamente estes números.
 */
final class MetasSecao84 {

    private MetasSecao84() {
    }

    //                                            C1    C2    C3    C4    C5    C6
    static final SerieDoTime ATENDIMENTO = serie(
            new double[]{55, 61, 67, 72, 73, 75},       // técnico
            new double[]{63, 59, 53, 47, 50, 56},       // confiança
            new double[]{96, 92, 92, 79, 83, 92},       // engajamento
            new double[]{60, 63, 65, 67, 73, 78});      // acerto em verificação (C4 = 67, decisão D02)

    static final SerieDoTime FINANCEIRO = serie(
            new double[]{58, 61, 65, 69, 72, 75},
            new double[]{34, 41, 47, 53, 59, 63},
            new double[]{83, 88, 88, 92, 92, 96},
            new double[]{53, 55, 60, 65, 68, 73});

    static final SerieDoTime MARKETING = serie(
            new double[]{64, 65, 66, 67, 68, 69},
            new double[]{78, 81, 84, 84, 88, 88},
            new double[]{100, 96, 100, 96, 100, 100},
            new double[]{38, 40, 40, 43, 43, 45});

    static PontoDoCiclo ponto(int ciclo, double tecnico, Double confianca, double engajamento, Double verificacao) {
        return new PontoDoCiclo(ciclo, BigDecimal.valueOf(tecnico),
                confianca == null ? null : BigDecimal.valueOf(confianca),
                BigDecimal.valueOf(engajamento),
                verificacao == null ? null : BigDecimal.valueOf(verificacao));
    }

    private static SerieDoTime serie(double[] tecnico, double[] confianca, double[] engajamento, double[] verificacao) {
        List<PontoDoCiclo> pontos = new ArrayList<>();
        for (int i = 0; i < tecnico.length; i++) {
            pontos.add(ponto(i + 1, tecnico[i], confianca[i], engajamento[i], verificacao[i]));
        }
        return SerieDoTime.de(pontos);
    }
}
