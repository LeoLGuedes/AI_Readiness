package br.com.prontidaoia.diagnostico.indicador;

import br.com.prontidaoia.comum.Arredondamento;
import java.util.List;

/**
 * Seção 5.1, linha 3. <b>Resistência</b> = média de
 * (receio_substituicao − 1) / 4 × 100 nos check-ins do ciclo.
 */
public final class CalculoResistencia {

    private CalculoResistencia() {
    }

    public static double calcular(List<Integer> respostasPergunta2) {
        return respostasPergunta2.stream()
                .mapToDouble(Arredondamento::escalaParaCem)
                .average()
                .orElse(0.0);
    }
}
