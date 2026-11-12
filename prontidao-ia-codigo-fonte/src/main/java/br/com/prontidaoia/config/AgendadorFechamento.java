package br.com.prontidaoia.config;

import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.diagnostico.fechamento.FechamentoCicloService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Dispara o fechamento quando a data de fim do ciclo aberto já passou
 * (seção 5.5). O painel de demonstração dispara o mesmo serviço à mão.
 *
 * <p>Desligável por {@code prontidao.ciclos.fechamento-automatico}; os testes
 * o desligam para que nada feche um ciclo no meio de uma verificação.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "prontidao.ciclos", name = "fechamento-automatico", havingValue = "true",
        matchIfMissing = true)
public class AgendadorFechamento {

    private final CicloService ciclos;
    private final FechamentoCicloService fechamento;
    private final Clock clock;

    public AgendadorFechamento(CicloService ciclos, FechamentoCicloService fechamento, Clock clock) {
        this.ciclos = ciclos;
        this.fechamento = fechamento;
        this.clock = clock;
    }

    /** Confere a cada hora, no minuto 5. */
    @Scheduled(cron = "0 5 * * * *")
    public void fecharSeVenceu() {
        LocalDate hoje = LocalDate.now(clock);
        if (ciclos.cicloAberto().filter(c -> c.venceuEm(hoje)).isPresent()) {
            fechamento.fecharCicloAberto();
        }
    }
}
