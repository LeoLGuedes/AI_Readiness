package br.com.prontidaoia.simulacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.checkin.CheckinService;
import br.com.prontidaoia.checkin.RespostaCheckin;
import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.colaborador.HistoricoPessoalService;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.PrioridadeAlerta;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.alerta.TipoAlerta;
import br.com.prontidaoia.diagnostico.escopo.EscopoService;
import br.com.prontidaoia.diagnostico.fechamento.FechamentoCicloService;
import br.com.prontidaoia.diagnostico.indicador.CalculoErroPorArea;
import br.com.prontidaoia.diagnostico.indicador.ColetaDoCiclo;
import br.com.prontidaoia.diagnostico.indicador.IndicadorRepository;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import br.com.prontidaoia.diagnostico.indicador.RespostaAvaliada;
import br.com.prontidaoia.diagnostico.recomendacao.Recomendacao;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoService;
import br.com.prontidaoia.simulacao.PerfilTime.Metas;
import br.com.prontidaoia.usuario.Papel;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioDetailsService;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * As 10 invariantes da seção 8.5: restaura o cenário, executa o roteiro da
 * Carla e avança os ciclos, conferindo cada critério de aceite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
@DisplayName("Invariantes do cenário (seção 8.5)")
class InvariantesDoCenarioTest extends TesteComBanco {

    private static final String ATENDIMENTO = PessoasDemo.TIME_ATENDIMENTO;
    private static final String FINANCEIRO = PessoasDemo.TIME_FINANCEIRO;
    private static final String MARKETING = PessoasDemo.TIME_MARKETING;

    @Autowired private CenarioInicial cenario;
    @Autowired private PreenchimentoCarla carla;
    @Autowired private GeradorDeterministico gerador;
    @Autowired private FechamentoCicloService fechamento;
    @Autowired private RecomendacaoService decisoes;
    @Autowired private EscopoService escopo;
    @Autowired private AlertaRepository alertas;
    @Autowired private RecomendacaoRepository recomendacoes;
    @Autowired private IndicadorRepository indicadores;
    @Autowired private ColetaDoCiclo coleta;
    @Autowired private CicloRepository ciclos;
    @Autowired private TimeRepository times;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CheckinService checkins;
    @Autowired private HistoricoPessoalService historico;
    @Autowired private UsuarioDetailsService detalhes;
    @Autowired private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rotas;

    // ------------------------------------------------------------------
    // Roteiro
    // ------------------------------------------------------------------

    /** Passos 1 a 3 da seção 9: restaurar, roteiro da Carla, fechar o ciclo 4. */
    private void ateOFechamentoDoCiclo4() {
        cenario.restaurar();
        carla.preencher();
        fechamento.fecharCicloAberto();
    }

    private void avancarUmCiclo() {
        gerador.gerarCicloAberto();
        fechamento.fecharCicloAberto();
    }

    private TimeEntity time(String nome) {
        return times.findByNome(nome).orElseThrow();
    }

    private List<Alerta> alertasDo(String nome) {
        return alertas.findByTimeIdOrderById(time(nome).getId());
    }

    private List<Alerta> alertasDoTipo(String nome, TipoAlerta tipo) {
        return alertasDo(nome).stream().filter(a -> a.getTipo() == tipo).toList();
    }

    private IndicadorTimeCiclo indicador(String nome, int ciclo) {
        return indicadores.findByTimeIdAndCicloId(time(nome).getId(),
                ciclos.findByNumero(ciclo).orElseThrow().getId()).orElseThrow();
    }

    private Alerta alertaA() {
        List<Alerta> doTipo = alertasDoTipo(ATENDIMENTO, TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA);
        assertThat(doTipo).hasSize(1);
        return doTipo.getFirst();
    }

    // ------------------------------------------------------------------
    // Invariantes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("1 a 7: alertas, confiança, erro por área e recomendação depois do ciclo 4")
    void invariantesDoCiclo4() {
        cenario.restaurar();

        // 1 (parte): até o ciclo 3, nenhum alerta A em time nenhum.
        assertThat(alertas.findAll()).noneMatch(a -> a.getTipo() == TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA);

        carla.preencher();
        fechamento.fecharCicloAberto();

        // 1. A regra A dispara para o Atendimento no fechamento do ciclo 4.
        Alerta a = alertaA();
        assertThat(a.getCiclo().getNumero()).isEqualTo(4);
        assertThat(alertasDoTipo(FINANCEIRO, TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA)).isEmpty();
        assertThat(alertasDoTipo(MARKETING, TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA)).isEmpty();

        // 2. O alerta A nasce com prioridade ALTA.
        assertThat(a.getPrioridade()).isEqualTo(PrioridadeAlerta.ALTA);

        // 3. Um único alerta B aberto para o Marketing, desde o ciclo 2.
        List<Alerta> doMarketing = alertasDoTipo(MARKETING, TipoAlerta.CONFIANCA_SEM_VERIFICACAO);
        assertThat(doMarketing).hasSize(1);
        assertThat(doMarketing.getFirst().getCiclo().getNumero()).isEqualTo(2);
        assertThat(doMarketing.getFirst().getStatus()).isEqualTo(StatusAlerta.ABERTO);
        assertThat(alertasDoTipo(ATENDIMENTO, TipoAlerta.CONFIANCA_SEM_VERIFICACAO)).isEmpty();

        // 4. O Financeiro não tem alerta.
        assertThat(alertasDo(FINANCEIRO)).isEmpty();

        // 5. Confiança: no ciclo 1 o Atendimento está acima do Financeiro; no ciclo 4, abaixo.
        assertThat(indicador(ATENDIMENTO, 1).getConfianca()).isGreaterThan(indicador(FINANCEIRO, 1).getConfianca());
        assertThat(indicador(ATENDIMENTO, 4).getConfianca()).isLessThan(indicador(FINANCEIRO, 4).getConfianca());

        // 6. No ciclo 4, VERIFICACAO é a área com maior erro da empresa, e o Marketing é quem mais erra nela.
        Long ciclo4 = ciclos.findByNumero(4).orElseThrow().getId();
        List<RespostaAvaliada> daEmpresa = new ArrayList<>();
        Map<String, Double> erroEmVerificacao = new HashMap<>();
        for (String nome : List.of(ATENDIMENTO, FINANCEIRO, MARKETING)) {
            List<RespostaAvaliada> doTime = coleta.respostas(time(nome).getId(), ciclo4);
            daEmpresa.addAll(doTime);
            erroEmVerificacao.put(nome, CalculoErroPorArea.erroPorArea(doTime).get("VERIFICACAO"));
        }
        Map<String, Double> erroDaEmpresa = CalculoErroPorArea.erroPorArea(daEmpresa);
        String maiorErro = erroDaEmpresa.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        assertThat(maiorErro).isEqualTo("VERIFICACAO");
        String quemMaisErra = erroEmVerificacao.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        assertThat(quemMaisErra).isEqualTo(MARKETING);

        // 7. A recomendação do alerta A tem exatamente dois itens.
        List<Recomendacao> itens = recomendacoes.doAlerta(a.getId());
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).getDificuldade().getCodigo()).isEqualTo("RECEIO_SUBSTITUICAO");
        assertThat(itens.get(0).getNRespostas()).isEqualTo(5);
        assertThat(itens.get(0).getTotalRespostas()).isEqualTo(7);
        assertThat(itens.get(0).getIntervencao().getCodigo()).isEqualTo("COMUNICACAO_LIDERANCA");
        assertThat(itens.get(1).getDificuldade().getCodigo()).isEqualTo("NAO_SEI_QUANDO_CONFIAR");
        assertThat(itens.get(1).getNRespostas()).isEqualTo(3);
        assertThat(itens.get(1).getTotalRespostas()).isEqualTo(7);
        assertThat(itens.get(1).getIntervencao().getCodigo()).isEqualTo("TRILHA_VERIFICACAO");
    }

    @Test
    @DisplayName("8: escopo salvo no início do ciclo 5 → em acompanhamento após o 5, resolvido no 6")
    void invariante8() {
        ateOFechamentoDoCiclo4();
        Alerta a = alertaA();
        Usuario renata = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.GESTORA)).orElseThrow();

        decisoes.aceitar(a.getId(), renata.getId());
        EscopoService.EscopoSalvo salvo = escopo.salvar(a.getId());
        assertThat(salvo.noCicloAberto()).isTrue();
        assertThat(salvo.cicloDeAplicacao()).isEqualTo(5);

        avancarUmCiclo();
        assertThat(alertas.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(StatusAlerta.EM_ACOMPANHAMENTO);

        avancarUmCiclo();
        Alerta depois = alertas.findById(a.getId()).orElseThrow();
        assertThat(depois.getStatus()).isEqualTo(StatusAlerta.RESOLVIDO);
        assertThat(depois.getCicloResolucao().getNumero()).isEqualTo(6);

        // 3 e 4 continuam valendo até o fim: um só alerta B no Marketing, nenhum no Financeiro.
        assertThat(alertasDoTipo(MARKETING, TipoAlerta.CONFIANCA_SEM_VERIFICACAO)).hasSize(1)
                .first().extracting(Alerta::getStatus).isEqualTo(StatusAlerta.ABERTO);
        assertThat(alertasDo(FINANCEIRO)).isEmpty();
        assertThat(alertasDoTipo(ATENDIMENTO, TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA)).hasSize(1);
    }

    @Test
    @DisplayName("9: nenhuma rota do gestor devolve dado de uma pessoa específica")
    void invariante9() throws Exception {
        ateOFechamentoDoCiclo4();
        Usuario renata = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.GESTORA)).orElseThrow();
        List<String> colaboradores = usuarios.findByPapelOrderByNome(Papel.COLABORADOR).stream()
                .map(Usuario::getNome).toList();
        String idQualquer = time(ATENDIMENTO).getId().toString();

        List<String> rotasDoGestor = new ArrayList<>();
        rotas.getHandlerMethods().forEach((info, metodo) -> {
            boolean get = info.getMethodsCondition().getMethods().isEmpty()
                    || info.getMethodsCondition().getMethods().contains(RequestMethod.GET);
            if (get && info.getPathPatternsCondition() != null) {
                info.getPathPatternsCondition().getPatternValues().stream()
                        .filter(p -> p.startsWith("/gestao"))
                        .map(p -> p.replaceAll("\\{[^}]+}", idQualquer))
                        .forEach(rotasDoGestor::add);
            }
        });
        assertThat(rotasDoGestor).isNotEmpty();

        for (String rota : rotasDoGestor) {
            String corpo = mockMvc.perform(get(rota).with(user(detalhes.loadUserByUsername(renata.getEmail()))))
                    .andReturn().getResponse().getContentAsString();
            assertThat(colaboradores).as("rota %s", rota).noneMatch(corpo::contains);
        }
    }

    @Test
    @DisplayName("10: com menos de 5 check-ins, confiança e resistência ficam nulas e as regras não valem (dados próprios)")
    void invariante10() {
        // Parte do ciclo 1 vazio, não do cenário.
        TimeEntity marketing = time(MARKETING);
        List<Usuario> membros = usuarios.findByTimeIdOrderByNome(marketing.getId());
        for (int ciclo = 1; ciclo <= 2; ciclo++) {
            for (int i = 0; i < 4; i++) {
                checkins.registrar(membros.get(i).getId(), new RespostaCheckin(5, 1, List.of(), true));
            }
            fechamento.fecharCicloAberto();
        }

        IndicadorTimeCiclo semAmostra = indicador(MARKETING, 2);
        assertThat(semAmostra.getNCheckins()).isEqualTo(4);
        assertThat(semAmostra.getConfianca()).isNull();
        assertThat(semAmostra.getResistencia()).isNull();
        assertThat(alertasDo(MARKETING)).isEmpty();
    }

    // ------------------------------------------------------------------
    // Metas da seção 8.4
    // ------------------------------------------------------------------

    @Test
    @DisplayName("os 6 ciclos gerados ficam dentro da tolerância da seção 8.4 (±2 pontos, ±0,5 min)")
    void metasDentroDaTolerancia() {
        ateOFechamentoDoCiclo4();
        avancarUmCiclo();
        avancarUmCiclo();

        for (String nome : List.of(ATENDIMENTO, FINANCEIRO, MARKETING)) {
            Map<Integer, Double> verificacao = coleta.acertoEmVerificacaoPorCiclo(time(nome).getId());
            for (int c = 1; c <= 6; c++) {
                Metas meta = PerfilTime.de(nome, c);
                IndicadorTimeCiclo i = indicador(nome, c);
                String onde = nome + ", ciclo " + c + ": ";
                assertThat(d(i.getTecnico())).as(onde + "técnico").isCloseTo(meta.tecnico(), within(2.0));
                assertThat(d(i.getConfianca())).as(onde + "confiança").isCloseTo(meta.confianca(), within(2.0));
                assertThat(d(i.getResistencia())).as(onde + "resistência").isCloseTo(meta.resistencia(), within(2.0));
                assertThat(d(i.getEngajamento())).as(onde + "engajamento").isCloseTo(meta.engajamento(), within(2.0));
                assertThat(d(i.getTempoMedioMin())).as(onde + "tempo").isCloseTo(meta.tempoMin(), within(0.5));
                assertThat(verificacao.get(c)).as(onde + "verificação").isCloseTo(meta.verificacao(), within(2.0));
            }
            Metas c4 = PerfilTime.de(nome, 4);
            Map<String, Double> acerto = CalculoErroPorArea.acertoPorArea(
                    coleta.respostas(time(nome).getId(), ciclos.findByNumero(4).orElseThrow().getId()));
            c4.acertoPorArea().forEach((area, esperado) ->
                    assertThat(acerto.get(area)).as(nome + ", ciclo 4, " + area).isCloseTo(esperado, within(2.0)));
        }

        // Atendimento, ciclo 4: 7 de 8 check-ins e 12 de 16 atividades (19 de 24 pendências).
        IndicadorTimeCiclo atendimento4 = indicador(ATENDIMENTO, 4);
        assertThat(atendimento4.getNCheckins()).isEqualTo(7);
        assertThat(atendimento4.getEngajamento()).isEqualByComparingTo("79.2");
        assertThat(coleta.contagemDeDificuldades(time(ATENDIMENTO).getId(),
                ciclos.findByNumero(4).orElseThrow().getId()).values())
                .containsExactlyInAnyOrder(5L, 3L, 2L, 1L);
    }

    @Test
    @DisplayName("seção 8.3: no ciclo 3, o acerto pessoal da Carla é 65%")
    void historicoDaCarla() {
        cenario.restaurar();
        Usuario carlaMenezes = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.CARLA)).orElseThrow();

        var ciclo3 = historico.daPessoa(carlaMenezes.getId()).get(2);
        assertThat(ciclo3.ciclo()).isEqualTo(3);
        assertThat(ciclo3.acertoPercentual()).isEqualTo(65);
    }

    @Test
    @DisplayName("restaurar duas vezes produz exatamente os mesmos indicadores (simulação determinística)")
    void deterministico() {
        cenario.restaurar();
        List<String> primeira = fotografiaDosTresCiclos();
        cenario.restaurar();
        assertThat(fotografiaDosTresCiclos()).isEqualTo(primeira);
    }

    private List<String> fotografiaDosTresCiclos() {
        List<String> linhas = new ArrayList<>();
        for (String nome : List.of(ATENDIMENTO, FINANCEIRO, MARKETING)) {
            for (int c = 1; c <= 3; c++) {
                IndicadorTimeCiclo i = indicador(nome, c);
                linhas.add(nome + c + ":" + i.getTecnico() + "/" + i.getConfianca() + "/" + i.getResistencia()
                        + "/" + i.getEngajamento() + "/" + i.getTempoMedioMin());
            }
        }
        return linhas;
    }

    private static double d(BigDecimal valor) {
        return valor.doubleValue();
    }
}
