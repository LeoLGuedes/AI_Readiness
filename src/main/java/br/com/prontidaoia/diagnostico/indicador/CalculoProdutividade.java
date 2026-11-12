package br.com.prontidaoia.diagnostico.indicador;

import java.time.Duration;
import java.util.List;

/**
 * Seção 5.1, linha 5. <b>Produtividade</b> = tempo médio
 * (finalizada_em − iniciada_em) das tentativas finalizadas, em minutos.
 *
 * <p>É lida junto com o técnico, que representa a qualidade: ficar mais
 * rápido só é ganho se o acerto não cair.</p>
 */
public final class CalculoProdutividade {

    private CalculoProdutividade() {
    }

    /** Sem tentativa finalizada, o tempo médio é 0 (decisão D36). */
    public static double minutosPorAtividade(List<Duration> duracoes) {
        return duracoes.stream()
                .mapToDouble(d -> d.toMillis() / 60_000.0)
                .average()
                .orElse(0.0);
    }
}
