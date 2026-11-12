package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.config.CiclosProperties;
import br.com.prontidaoia.diagnostico.fechamento.FechamentoCicloService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Restaurar cenário inicial" (seção 8.2).
 *
 * <ul>
 *   <li>apaga todo o movimento (check-ins, tentativas, ciclos, indicadores,
 *       alertas, recomendações) e mantém pessoas e catálogos;</li>
 *   <li>gera e <b>fecha pelo serviço de fechamento</b> os ciclos 1, 2 e 3;</li>
 *   <li>deixa o ciclo 4 aberto, contendo a data de hoje, com as respostas dos
 *       23 colaboradores, menos as da Carla; um membro do Atendimento pula o
 *       check-in, para o time fechar com 7 de 8.</li>
 * </ul>
 */
@Component
public class CenarioInicial {

    /** Dias do ciclo 4 que já passaram quando o cenário é restaurado. */
    static final int DIAS_DO_CICLO_4_JA_PASSADOS = 3;

    private final EntityManager em;
    private final SeedPessoas seedPessoas;
    private final SeedAtividades seedAtividades;
    private final CicloService ciclos;
    private final GeradorDeterministico gerador;
    private final FechamentoCicloService fechamento;
    private final CiclosProperties configCiclos;
    private final Clock clock;

    public CenarioInicial(EntityManager em, SeedPessoas seedPessoas, SeedAtividades seedAtividades,
                          CicloService ciclos, GeradorDeterministico gerador, FechamentoCicloService fechamento,
                          CiclosProperties configCiclos, Clock clock) {
        this.em = em;
        this.seedPessoas = seedPessoas;
        this.seedAtividades = seedAtividades;
        this.ciclos = ciclos;
        this.gerador = gerador;
        this.fechamento = fechamento;
        this.configCiclos = configCiclos;
        this.clock = clock;
    }

    @Transactional
    public void restaurar() {
        em.flush();
        em.createNativeQuery("""
                truncate table resposta, tentativa, checkin_dificuldade, checkin, atividade_liberada,
                               recomendacao, alerta, indicador_time_ciclo, ciclo""")
                .executeUpdate();
        em.clear();

        seedPessoas.semear();
        seedAtividades.semear();

        // O ciclo 4 começa três dias atrás, para conter hoje sem que o
        // agendador o feche no meio da demonstração.
        LocalDate inicioDoCiclo4 = LocalDate.now(clock).minusDays(DIAS_DO_CICLO_4_JA_PASSADOS);
        ciclos.abrirPrimeiroCiclo(inicioDoCiclo4.minusDays(3L * configCiclos.diasPorCiclo()));

        for (int ciclo = 1; ciclo <= 3; ciclo++) {
            gerador.gerarCicloAberto();
            fechamento.fecharCicloAberto();
        }
        gerador.gerarCicloAberto();
    }
}
