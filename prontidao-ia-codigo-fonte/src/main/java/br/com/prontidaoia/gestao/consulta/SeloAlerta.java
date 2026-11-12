package br.com.prontidaoia.gestao.consulta;

import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.PrioridadeAlerta;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;

/**
 * Selo de alerta: sempre com o nome escrito, nunca só a cor (seção 7).
 * Vermelho para prioridade alta, âmbar para normal.
 */
public record SeloAlerta(Long id, String nome, PrioridadeAlerta prioridade, StatusAlerta status) {

    public static SeloAlerta de(Alerta a) {
        return new SeloAlerta(a.getId(), a.getTipo().getNomeExibido(), a.getPrioridade(), a.getStatus());
    }

    public String classe() {
        return prioridade == PrioridadeAlerta.ALTA ? "badge-danger" : "badge-warning";
    }

    public String rotuloPrioridade() {
        return prioridade == PrioridadeAlerta.ALTA ? "Alta" : "Normal";
    }

    public String rotuloStatus() {
        return switch (status) {
            case ABERTO -> "aberto";
            case EM_ACOMPANHAMENTO -> "em acompanhamento";
            case RESOLVIDO -> "resolvido";
        };
    }
}
