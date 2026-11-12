package br.com.prontidaoia.diagnostico.alerta;

import java.util.Optional;

/**
 * <b>Encerramento do alerta B</b> (seção 5.2): quando a condição B deixa de
 * valer por dois ciclos seguidos.
 *
 * <p>"Deixar de valer" é avaliado ciclo a ciclo com
 * {@link RegraConfiancaSemVerificacao#condicaoNoCiclo}. Um ciclo sem amostra
 * mínima não prova nada, então não conta como "deixou de valer" (decisão D37).</p>
 */
public final class RegraEncerramentoConfianca {

    private final int ciclosSeguidos;
    private final RegraConfiancaSemVerificacao regraB;

    public RegraEncerramentoConfianca(int ciclosParaEncerramento, RegraConfiancaSemVerificacao regraB) {
        this.ciclosSeguidos = ciclosParaEncerramento;
        this.regraB = regraB;
    }

    public boolean deveEncerrar(SerieDoTime serie, int c) {
        for (int k = c - ciclosSeguidos + 1; k <= c; k++) {
            Optional<PontoDoCiclo> ponto = serie.no(k);
            if (ponto.isEmpty() || !ponto.get().temAmostra() || regraB.condicaoNoCiclo(ponto.get())) {
                return false;
            }
        }
        return true;
    }
}
