package br.com.prontidaoia.diagnostico.alerta;

import java.util.Optional;

/**
 * <b>Encerramento do alerta A</b> (seção 5.2): um alerta EM_ACOMPANHAMENTO
 * passa a RESOLVIDO quando a confiança sobe em dois ciclos seguidos a partir
 * do ciclo de aplicação da intervenção, comparando cada ciclo com o anterior.
 *
 * <p>No cenário, a intervenção vale no ciclo 5: a confiança vai de 47 (C4)
 * para 50 (C5) e 56 (C6). Depois do ciclo 5 houve só uma subida; o alerta é
 * resolvido no fechamento do ciclo 6, e não antes.</p>
 */
public final class RegraEncerramentoDivergencia {

    private final int ciclosSeguidos;

    public RegraEncerramentoDivergencia(int ciclosParaEncerramento) {
        this.ciclosSeguidos = ciclosParaEncerramento;
    }

    /**
     * @param c               ciclo que está sendo fechado
     * @param cicloAplicacao  primeiro ciclo em que a intervenção valeu
     */
    public boolean deveEncerrar(SerieDoTime serie, int c, int cicloAplicacao) {
        int primeiro = c - ciclosSeguidos + 1;
        if (primeiro < cicloAplicacao) {
            return false;
        }
        for (int k = primeiro; k <= c; k++) {
            if (!subiu(serie.no(k), serie.no(k - 1))) {
                return false;
            }
        }
        return true;
    }

    private static boolean subiu(Optional<PontoDoCiclo> ciclo, Optional<PontoDoCiclo> anterior) {
        return ciclo.isPresent() && anterior.isPresent()
                && ciclo.get().temAmostra() && anterior.get().temAmostra()
                && ciclo.get().confianca().compareTo(anterior.get().confianca()) > 0;
    }
}
