package br.com.prontidaoia.diagnostico.alerta;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * <b>Prioridade</b> (seção 5.2): o alerta nasce com prioridade ALTA se
 * engajamento(c−1) − engajamento(c) ≥ 10. Caso contrário, NORMAL.
 *
 * <p>No cenário, o Atendimento cai de 92 para 79 no ciclo 4 (13 pontos): ALTA.</p>
 */
public final class RegraPrioridade {

    private final BigDecimal quedaParaAlta;

    public RegraPrioridade(int quedaEngajamentoPrioridadeAlta) {
        this.quedaParaAlta = BigDecimal.valueOf(quedaEngajamentoPrioridadeAlta);
    }

    public PrioridadeAlerta prioridade(SerieDoTime serie, int c) {
        Optional<PontoDoCiclo> atual = serie.no(c);
        Optional<PontoDoCiclo> anterior = serie.no(c - 1);
        if (atual.isEmpty() || anterior.isEmpty()) {
            return PrioridadeAlerta.NORMAL;
        }
        BigDecimal queda = anterior.get().engajamento().subtract(atual.get().engajamento());
        return queda.compareTo(quedaParaAlta) >= 0 ? PrioridadeAlerta.ALTA : PrioridadeAlerta.NORMAL;
    }
}
