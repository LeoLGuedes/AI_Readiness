package br.com.prontidaoia.diagnostico.indicador;

import br.com.prontidaoia.comum.Arredondamento;
import java.util.List;

/**
 * Seção 5.1, linha 2. <b>Confiança</b> = média de (confianca − 1) / 4 × 100
 * nos check-ins do ciclo.
 *
 * <p>Esta classe só faz a conta. Quem decide se há amostra para mostrar o
 * resultado é {@link AmostraMinima}.</p>
 */
public final class CalculoConfianca {

    private CalculoConfianca() {
    }

    public static double calcular(List<Integer> respostasPergunta1) {
        return respostasPergunta1.stream()
                .mapToDouble(Arredondamento::escalaParaCem)
                .average()
                .orElse(0.0);
    }
}
