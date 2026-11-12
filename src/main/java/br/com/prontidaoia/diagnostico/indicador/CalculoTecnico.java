package br.com.prontidaoia.diagnostico.indicador;

import java.util.List;

/**
 * Seção 5.1, linha 1. <b>Técnico</b> = % de respostas corretas nas tentativas
 * finalizadas no ciclo.
 */
public final class CalculoTecnico {

    private CalculoTecnico() {
    }

    /** Sem nenhuma tentativa finalizada, o técnico é 0 (decisão D36). */
    public static double calcular(List<RespostaAvaliada> respostas) {
        if (respostas.isEmpty()) {
            return 0.0;
        }
        long corretas = respostas.stream().filter(RespostaAvaliada::correta).count();
        return corretas * 100.0 / respostas.size();
    }
}
