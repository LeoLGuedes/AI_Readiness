package br.com.prontidaoia.diagnostico.alerta;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.usuario.TimeEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistência dos alertas: criação, a regra "sem duplicatas" e o encerramento.
 * As regras em si ficam nas classes {@code Regra*}, sem banco.
 */
@Service
public class AlertaService {

    private final AlertaRepository alertas;

    public AlertaService(AlertaRepository alertas) {
        this.alertas = alertas;
    }

    /** O que aconteceu ao registrar um disparo. */
    public record Registro(Alerta alerta, boolean novo) {
    }

    /**
     * Registra um disparo de regra. <b>Sem duplicatas</b> (seção 5.2): se o
     * time já tem um alerta não resolvido do mesmo tipo, só o detalhe é
     * atualizado; a prioridade e o ciclo de origem continuam os do nascimento.
     */
    @Transactional
    public Registro registrar(TimeEntity time, Ciclo ciclo, DisparoDeRegra disparo, PrioridadeAlerta prioridade) {
        return alertas.naoResolvidoDoTipo(time.getId(), disparo.tipo())
                .map(existente -> {
                    existente.atualizarDetalhe(disparo.detalhe());
                    return new Registro(existente, false);
                })
                .orElseGet(() -> new Registro(
                        alertas.save(new Alerta(time, ciclo, disparo.tipo(), prioridade, disparo.detalhe())),
                        true));
    }

    @Transactional
    public void resolver(Alerta alerta, Ciclo noFechamentoDe) {
        alerta.resolver(noFechamentoDe);
        // Grava já: um alerta novo do mesmo tipo pode nascer no mesmo fechamento,
        // e o índice único só aceita se este já estiver RESOLVIDO no banco.
        alertas.saveAndFlush(alerta);
    }
}
