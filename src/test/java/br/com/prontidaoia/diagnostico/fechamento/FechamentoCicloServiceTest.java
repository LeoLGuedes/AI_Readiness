package br.com.prontidaoia.diagnostico.fechamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.ExecucaoAtividadeService;
import br.com.prontidaoia.atividade.Questao;
import br.com.prontidaoia.atividade.QuestaoRepository;
import br.com.prontidaoia.checkin.CheckinService;
import br.com.prontidaoia.checkin.Dificuldade;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.checkin.RespostaCheckin;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.ciclo.StatusCiclo;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.PrioridadeAlerta;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.alerta.TipoAlerta;
import br.com.prontidaoia.diagnostico.indicador.IndicadorRepository;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import br.com.prontidaoia.diagnostico.recomendacao.Recomendacao;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fechamento do ciclo (seção 5.5) com dados próprios, gerados pelos serviços
 * de check-in e de atividade, como a colaboradora faria. Nenhum indicador ou
 * alerta é inserido à mão: tudo sai do fechamento.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
@DisplayName("Fechamento do ciclo")
class FechamentoCicloServiceTest extends TesteComBanco {

    @Autowired private FechamentoCicloService fechamento;
    @Autowired private CicloService cicloService;
    @Autowired private CicloRepository ciclos;
    @Autowired private CheckinService checkins;
    @Autowired private ExecucaoAtividadeService execucao;
    @Autowired private AtividadeLiberadaRepository liberadas;
    @Autowired private QuestaoRepository questoes;
    @Autowired private DificuldadeRepository dificuldades;
    @Autowired private TimeRepository times;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private IndicadorRepository indicadores;
    @Autowired private AlertaRepository alertas;
    @Autowired private RecomendacaoRepository recomendacoes;

    /**
     * Preenche o ciclo aberto para um time.
     *
     * @param checkinsFeitos      quantas pessoas do time fazem o check-in
     * @param confianca           resposta de todas à pergunta 1
     * @param marcacoes           código da dificuldade → quantas das pessoas a marcam
     * @param acertosFormulacao   acertos nas questões 1 a 5 da atividade A (uma pessoa)
     * @param acertosVerificacao  acertos nas questões 6 a 10 da atividade A
     */
    private void preencher(String nomeDoTime, int checkinsFeitos, int confianca, Map<String, Integer> marcacoes,
                           int acertosFormulacao, int acertosVerificacao) {
        TimeEntity time = times.findByNome(nomeDoTime).orElseThrow();
        List<Usuario> membros = usuarios.findByTimeIdOrderByNome(time.getId());
        Map<String, Long> idPorCodigo = new HashMap<>();
        dificuldades.findAllByOrderById().forEach(d -> idPorCodigo.put(d.getCodigo(), d.getId()));

        for (int i = 0; i < checkinsFeitos; i++) {
            List<Long> marcadas = new ArrayList<>();
            for (Map.Entry<String, Integer> m : marcacoes.entrySet()) {
                if (i < m.getValue()) {
                    marcadas.add(idPorCodigo.get(m.getKey()));
                }
            }
            checkins.registrar(membros.get(i).getId(),
                    new RespostaCheckin(confianca, 2, marcadas, marcadas.isEmpty()));
        }

        UsuarioAutenticado quemFaz = new UsuarioAutenticado(membros.getFirst());
        Ciclo aberto = cicloService.cicloAberto().orElseThrow();
        AtividadeLiberada atividadeA = liberadas.escopoDoTime(time.getId(), aberto.getId()).getFirst();
        execucao.abrir(quemFaz, atividadeA.getId());

        Map<Long, Integer> escolhas = new HashMap<>();
        for (Questao q : questoes.daAtividade(atividadeA.getAtividade().getId())) {
            boolean acerta = q.getOrdem() <= 5
                    ? q.getOrdem() <= acertosFormulacao
                    : q.getOrdem() - 5 <= acertosVerificacao;
            escolhas.put(q.getId(), acerta ? q.getIndiceCorreto() : (q.getIndiceCorreto() + 1) % 4);
        }
        execucao.finalizar(quemFaz, atividadeA.getId(), escolhas, Map.of());
    }

    private IndicadorTimeCiclo indicador(String nomeDoTime, int numeroDoCiclo) {
        Long time = times.findByNome(nomeDoTime).orElseThrow().getId();
        Long ciclo = ciclos.findByNumero(numeroDoCiclo).orElseThrow().getId();
        return indicadores.findByTimeIdAndCicloId(time, ciclo).orElseThrow();
    }

    private List<Alerta> alertasDo(String nomeDoTime) {
        return alertas.findByTimeIdOrderById(times.findByNome(nomeDoTime).orElseThrow().getId());
    }

    @Test
    @DisplayName("grava a fotografia de cada time, fecha o ciclo e abre o próximo com o escopo avançado")
    void gravaIndicadoresEAbreOProximo() {
        preencher(PessoasDemo.TIME_ATENDIMENTO, 5, 3, Map.of(), 4, 3);

        ResumoDoFechamento resumo = fechamento.fecharCicloAberto();

        IndicadorTimeCiclo atendimento = indicador(PessoasDemo.TIME_ATENDIMENTO, 1);
        assertThat(atendimento.getTecnico()).isEqualByComparingTo("70.0");      // 7 de 10
        assertThat(atendimento.getConfianca()).isEqualByComparingTo("50.0");    // todos 3
        assertThat(atendimento.getResistencia()).isEqualByComparingTo("25.0");  // todos 2
        assertThat(atendimento.getEngajamento()).isEqualByComparingTo("25.0");  // (5 + 1) / 24
        assertThat(atendimento.getNCheckins()).isEqualTo(5);
        assertThat(atendimento.getNMembros()).isEqualTo(8);

        IndicadorTimeCiclo financeiro = indicador(PessoasDemo.TIME_FINANCEIRO, 1);
        assertThat(financeiro.getConfianca()).isNull();
        assertThat(financeiro.getEngajamento()).isEqualByComparingTo("0.0");

        assertThat(ciclos.findByNumero(1).orElseThrow().getStatus()).isEqualTo(StatusCiclo.FECHADO);
        Ciclo segundo = cicloService.cicloAberto().orElseThrow();
        assertThat(segundo.getNumero()).isEqualTo(2);
        assertThat(resumo.cicloAberto()).isEqualTo(2);
        assertThat(liberadas.doCiclo(segundo.getId()))
                .extracting(l -> l.getAtividade().getCodigo())
                .containsOnly("2A", "2B")
                .hasSize(6);
    }

    @Test
    @DisplayName("com menos de 5 check-ins, confiança e resistência ficam nulas e as regras não são avaliadas")
    void amostraInsuficiente() {
        preencher(PessoasDemo.TIME_MARKETING, 4, 5, Map.of(), 5, 0);
        fechamento.fecharCicloAberto();
        preencher(PessoasDemo.TIME_MARKETING, 4, 5, Map.of(), 5, 0);
        fechamento.fecharCicloAberto();

        IndicadorTimeCiclo marketing = indicador(PessoasDemo.TIME_MARKETING, 2);
        assertThat(marketing.getConfianca()).isNull();
        assertThat(marketing.getResistencia()).isNull();
        assertThat(marketing.getNCheckins()).isEqualTo(4);
        // Com amostra, confiança 100 e verificação 0% dispararia a regra B.
        assertThat(alertasDo(PessoasDemo.TIME_MARKETING)).isEmpty();
    }

    @Test
    @DisplayName("regra A com prioridade alta e recomendações; regra B sem duplicatas; encerramento do A")
    void fluxoCompletoDeAlertas() {
        String atendimento = PessoasDemo.TIME_ATENDIMENTO;
        String financeiro = PessoasDemo.TIME_FINANCEIRO;
        String marketing = PessoasDemo.TIME_MARKETING;

        // Ciclo 1. Atendimento: técnico 50, confiança 75. Marketing: confiança 100, verificação 20%.
        preencher(atendimento, 5, 4, Map.of(), 2, 3);
        preencher(financeiro, 5, 2, Map.of(), 3, 3);
        preencher(marketing, 5, 5, Map.of(), 5, 1);
        fechamento.fecharCicloAberto();
        assertThat(alertas.findAll()).isEmpty();

        // Ciclo 2. Atendimento: técnico 70, confiança 50, com os 8 check-ins (engajamento 37,5).
        preencher(atendimento, 8, 3, Map.of(), 3, 4);
        preencher(financeiro, 5, 3, Map.of(), 3, 3);
        preencher(marketing, 5, 5, Map.of(), 5, 1);
        ResumoDoFechamento ciclo2 = fechamento.fecharCicloAberto();

        List<Alerta> doMarketing = alertasDo(marketing);
        assertThat(doMarketing).hasSize(1);
        Alerta alertaB = doMarketing.getFirst();
        assertThat(alertaB.getTipo()).isEqualTo(TipoAlerta.CONFIANCA_SEM_VERIFICACAO);
        assertThat(alertaB.getPrioridade()).isEqualTo(PrioridadeAlerta.NORMAL);
        assertThat(recomendacoes.doAlerta(alertaB.getId()))
                .extracting(r -> r.getIntervencao().getCodigo())
                .containsExactly("TRILHA_VERIFICACAO_CHECKLIST");
        assertThat(ciclo2.alertasNovos()).hasSize(1);

        // Ciclo 3. Atendimento: técnico 90, confiança 25, só 5 check-ins (engajamento 25: queda de 12,5).
        preencher(atendimento, 5, 2, Map.of("RECEIO_SUBSTITUICAO", 4, "NAO_SEI_QUANDO_CONFIAR", 2,
                "FALTA_TEMPO_SUPORTE", 2), 4, 5);
        preencher(financeiro, 5, 4, Map.of(), 3, 3);
        preencher(marketing, 5, 5, Map.of(), 5, 1);
        ResumoDoFechamento ciclo3 = fechamento.fecharCicloAberto();

        List<Alerta> doAtendimento = alertasDo(atendimento);
        assertThat(doAtendimento).hasSize(1);
        Alerta alertaA = doAtendimento.getFirst();
        assertThat(alertaA.getTipo()).isEqualTo(TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA);
        assertThat(alertaA.getPrioridade()).isEqualTo(PrioridadeAlerta.ALTA);
        assertThat(alertaA.getStatus()).isEqualTo(StatusAlerta.ABERTO);
        assertThat(alertaA.getDetalhe().frase())
                .isEqualTo("Técnico subiu de 50 para 70 e 90; confiança caiu de 75 para 50 e 25.");

        // Duas mais marcadas: receio (4 de 5) e, no empate de 2, "não sei confiar" vem antes no catálogo.
        List<Recomendacao> itens = recomendacoes.doAlerta(alertaA.getId());
        assertThat(itens).extracting(r -> r.getDificuldade().getCodigo())
                .containsExactly("RECEIO_SUBSTITUICAO", "NAO_SEI_QUANDO_CONFIAR");
        assertThat(itens).extracting(r -> r.getIntervencao().getCodigo())
                .containsExactly("COMUNICACAO_LIDERANCA", "TRILHA_VERIFICACAO");
        assertThat(itens).extracting(Recomendacao::getNRespostas).containsExactly(4, 2);
        assertThat(itens).extracting(Recomendacao::getTotalRespostas).containsExactly(5, 5);

        // Sem duplicatas: o alerta B do Marketing é o mesmo, com o detalhe do ciclo 3.
        assertThat(alertasDo(marketing)).hasSize(1).first()
                .satisfies(a -> assertThat(a.getDetalhe().ciclo()).isEqualTo(3));
        assertThat(ciclo3.alertasAtualizados()).containsExactly("Confiança alta sem senso crítico – Marketing");
        assertThat(alertasDo(financeiro)).isEmpty();

        // A gestão aplica a intervenção no ciclo 4 (o que a fatia 5 fará pela tela de escopo).
        Ciclo quarto = cicloService.cicloAberto().orElseThrow();
        alertaA.passarAAcompanhamento();
        itens.forEach(r -> r.registrarAplicacao(quarto));

        // Ciclo 4: confiança sobe (25 → 50). Uma subida só: continua em acompanhamento.
        preencher(atendimento, 5, 3, Map.of(), 4, 5);
        fechamento.fecharCicloAberto();
        assertThat(alertas.findById(alertaA.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusAlerta.EM_ACOMPANHAMENTO);

        // Ciclo 5: sobe de novo (50 → 75). Duas subidas seguidas: resolvido.
        preencher(atendimento, 5, 4, Map.of(), 4, 5);
        ResumoDoFechamento ciclo5 = fechamento.fecharCicloAberto();
        Alerta resolvido = alertas.findById(alertaA.getId()).orElseThrow();
        assertThat(resolvido.getStatus()).isEqualTo(StatusAlerta.RESOLVIDO);
        assertThat(resolvido.getCicloResolucao().getNumero()).isEqualTo(5);
        assertThat(ciclo5.alertasResolvidos()).containsExactly("Sabe usar, mas não se sente pronto – Atendimento");
    }

    @Test
    @DisplayName("o sexto ciclo fecha sem abrir outro; depois disso não há o que fechar")
    void ultimoCicloNaoAbreOutro() {
        for (int i = 1; i <= 5; i++) {
            assertThat(fechamento.fecharCicloAberto().cicloAberto()).isEqualTo(i + 1);
        }
        assertThat(fechamento.fecharCicloAberto().cicloAberto()).isNull();

        assertThat(cicloService.cicloAberto()).isEmpty();
        assertThatThrownBy(() -> fechamento.fecharCicloAberto())
                .isInstanceOf(RegraVioladaException.class)
                .hasMessage("Não há ciclo aberto para fechar.");
    }
}
