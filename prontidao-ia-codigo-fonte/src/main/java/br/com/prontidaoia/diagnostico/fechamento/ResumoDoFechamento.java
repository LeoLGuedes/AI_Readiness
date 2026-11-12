package br.com.prontidaoia.diagnostico.fechamento;

import java.util.List;

/**
 * O que aconteceu num fechamento, em frases curtas. Alimenta o registro de
 * ações do painel de demonstração e o log do agendador.
 */
public record ResumoDoFechamento(
        int cicloFechado,
        Integer cicloAberto,
        List<String> alertasNovos,
        List<String> alertasAtualizados,
        List<String> alertasResolvidos) {
}
