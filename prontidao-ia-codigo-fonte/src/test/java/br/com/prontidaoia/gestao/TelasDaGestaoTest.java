package br.com.prontidaoia.gestao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.ExecucaoAtividadeService;
import br.com.prontidaoia.checkin.CheckinService;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.checkin.RespostaCheckin;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.alerta.TipoAlerta;
import br.com.prontidaoia.diagnostico.fechamento.FechamentoCicloService;
import br.com.prontidaoia.diagnostico.recomendacao.Recomendacao;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.diagnostico.recomendacao.StatusRecomendacao;
import br.com.prontidaoia.simulacao.CenarioInicial;
import br.com.prontidaoia.simulacao.GeradorDeterministico;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.simulacao.PreenchimentoCarla;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import br.com.prontidaoia.usuario.UsuarioDetailsService;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Telas da gestão (Quadros 1, 2 e 3, e o escopo), percorridas como a Renata
 * fará nos passos 4 a 8 do roteiro da seção 9.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
@DisplayName("Telas da gestão")
class TelasDaGestaoTest extends TesteComBanco {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioDetailsService detalhes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TimeRepository times;
    @Autowired private CenarioInicial cenario;
    @Autowired private PreenchimentoCarla carla;
    @Autowired private GeradorDeterministico gerador;
    @Autowired private FechamentoCicloService fechamento;
    @Autowired private AlertaRepository alertas;
    @Autowired private RecomendacaoRepository recomendacoes;
    @Autowired private AtividadeLiberadaRepository liberadas;
    @Autowired private CicloService ciclos;
    @Autowired private ExecucaoAtividadeService execucao;
    @Autowired private CheckinService checkins;
    @Autowired private DificuldadeRepository dificuldades;

    private RequestPostProcessor renata() {
        return user(detalhes.loadUserByUsername(PessoasDemo.email(PessoasDemo.GESTORA)));
    }

    private Long timeId(String nome) {
        return times.findByNome(nome).orElseThrow().getId();
    }

    private String html(String rota) throws Exception {
        return mockMvc.perform(get(rota).with(renata()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Passos 1 a 3 do roteiro: cenário restaurado, Carla feita, ciclo 4 fechado. */
    private Alerta depoisDoCiclo4() {
        cenario.restaurar();
        carla.preencher();
        fechamento.fecharCicloAberto();
        return alertas.findByTimeIdOrderById(timeId(PessoasDemo.TIME_ATENDIMENTO)).stream()
                .filter(a -> a.getTipo() == TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA)
                .findFirst().orElseThrow();
    }

    private List<String> escopoDoAtendimentoNoCicloAberto() {
        Long ciclo = ciclos.cicloAberto().orElseThrow().getId();
        return liberadas.escopoDoTime(timeId(PessoasDemo.TIME_ATENDIMENTO), ciclo).stream()
                .map(l -> l.getAtividade().getCodigo()).toList();
    }

    @Test
    @DisplayName("Quadro 1: indicadores com variação, selos de alerta com nome, maior erro e maior ganho")
    void visaoGeral() throws Exception {
        depoisDoCiclo4();

        String pagina = html("/gestao");

        assertThat(pagina).contains(
                "Ciclo 4 ·",
                "Maior erro da empresa no ciclo 4:", "Verificação das respostas da IA",
                "Sabe usar, mas não se sente pronto · Alta",
                "Confiança alta sem senso crítico · Normal",
                "Sem alertas",
                "7 de 8 check-ins",
                "Financeiro: +19 pontos (34 → 53)");
    }

    @Test
    @DisplayName("Quadro 2: alerta explicado, dificuldades com % e aviso de amostra")
    void detalheDoTime() throws Exception {
        depoisDoCiclo4();

        String pagina = html("/gestao/times/" + timeId(PessoasDemo.TIME_ATENDIMENTO));

        assertThat(pagina).contains(
                "Gerado no fechamento do ciclo 4",
                "Técnico subiu de 61 para 67 e 72",
                "Engajamento caiu 13 pontos → prioridade alta.",
                "5 (71% de 7 respostas)",
                "3 (43% de 7 respostas)",
                "7 de 8 check-ins respondidos (mínimo de 5)",
                "Ver recomendação");
        assertThat(pagina).doesNotContain("Comparar antes e depois");
    }

    @Test
    @DisplayName("Quadro 3: dois itens com contagem, selos de executor e o rodapé")
    void recomendacao() throws Exception {
        Alerta a = depoisDoCiclo4();

        String pagina = html("/gestao/alertas/" + a.getId() + "/recomendacao");

        assertThat(pagina).contains(
                "Sabe usar, mas não se sente pronto – Atendimento – ciclo 4",
                "Receio de substituição · 5 de 7 respostas",
                "Não sei quando posso confiar na resposta · 3 de 7 respostas",
                "Ação humana", "Plataforma",
                "Por que esta recomendação?", "71% das respostas",
                "Aceitar e ajustar escopo", "Ajustar recomendação", "Adiar",
                "As recomendações apoiam a decisão da gestão e não a substituem.");
    }

    @Test
    @DisplayName("aceitar → escopo; salvar no início do ciclo 5 troca a 5A pela trilha e agenda a liderança")
    void aceitarESalvarEscopo() throws Exception {
        Alerta a = depoisDoCiclo4();
        Long atendimento = timeId(PessoasDemo.TIME_ATENDIMENTO);

        mockMvc.perform(post("/gestao/alertas/{id}/recomendacao/aceitar", a.getId()).with(renata()).with(csrf()))
                .andExpect(redirectedUrl("/gestao/times/" + atendimento + "/escopo"));
        assertThat(recomendacoes.doAlerta(a.getId()))
                .extracting(Recomendacao::getStatus).containsOnly(StatusRecomendacao.ACEITA);

        String escopo = html("/gestao/times/" + atendimento + "/escopo");
        assertThat(escopo).contains("Trocar 5A · Formulação e verificação → TRILHA-VERIFICACAO",
                "Salvar escopo", "a troca vale já no ciclo aberto");

        mockMvc.perform(post("/gestao/times/{id}/escopo", atendimento).with(renata()).with(csrf())
                        .param("alertaId", a.getId().toString()).param("aplicarTroca", "true"))
                .andExpect(redirectedUrl("/gestao/times/" + atendimento));

        assertThat(alertas.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(StatusAlerta.EM_ACOMPANHAMENTO);
        assertThat(escopoDoAtendimentoNoCicloAberto()).containsExactlyInAnyOrder("TRILHA-VERIFICACAO", "5B");

        Recomendacao comunicacao = recomendacoes.doAlerta(a.getId()).getFirst();
        mockMvc.perform(post("/gestao/recomendacoes/{id}/agendar", comunicacao.getId()).with(renata()).with(csrf())
                        .param("data", "2026-10-02"))
                .andExpect(redirectedUrl("/gestao/times/" + atendimento + "/escopo"));
        assertThat(recomendacoes.findById(comunicacao.getId()).orElseThrow().getDataPrevista())
                .isEqualTo(LocalDate.of(2026, 10, 2));

        // Depois do ciclo 5: o marcador de intervenção e o "Comparar antes e depois" aparecem.
        gerador.gerarCicloAberto();
        fechamento.fecharCicloAberto();
        String detalhe = html("/gestao/times/" + atendimento);
        assertThat(detalhe).contains("Comparar antes e depois", "C4 (antes)", "\"cicloIntervencao\":5");
    }

    @Test
    @DisplayName("FA1: o item com texto alterado fica AJUSTADO; o outro é aceito como está")
    void ajustarRecomendacao() throws Exception {
        Alerta a = depoisDoCiclo4();
        List<Recomendacao> itens = recomendacoes.doAlerta(a.getId());

        mockMvc.perform(post("/gestao/alertas/{id}/recomendacao/ajustar", a.getId()).with(renata()).with(csrf())
                        .param("texto" + itens.get(0).getId(), "Reunião da gerente com o time na segunda-feira.")
                        .param("texto" + itens.get(1).getId(), itens.get(1).textoVigente()))
                .andExpect(redirectedUrl("/gestao/times/" + timeId(PessoasDemo.TIME_ATENDIMENTO) + "/escopo"));

        Recomendacao ajustada = recomendacoes.findById(itens.get(0).getId()).orElseThrow();
        assertThat(ajustada.getStatus()).isEqualTo(StatusRecomendacao.AJUSTADA);
        assertThat(ajustada.getDescricaoAjustada()).isEqualTo("Reunião da gerente com o time na segunda-feira.");
        assertThat(ajustada.getDecididoPor()).isNotNull();
        assertThat(recomendacoes.findById(itens.get(1).getId()).orElseThrow().getStatus())
                .isEqualTo(StatusRecomendacao.ACEITA);
    }

    @Test
    @DisplayName("adiar: todos os itens ficam ADIADOS e o alerta continua aberto")
    void adiar() throws Exception {
        Alerta a = depoisDoCiclo4();

        mockMvc.perform(post("/gestao/alertas/{id}/recomendacao/adiar", a.getId()).with(renata()).with(csrf()))
                .andExpect(redirectedUrl("/gestao/times/" + timeId(PessoasDemo.TIME_ATENDIMENTO)));

        assertThat(recomendacoes.doAlerta(a.getId()))
                .extracting(Recomendacao::getStatus).containsOnly(StatusRecomendacao.ADIADA);
        assertThat(alertas.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(StatusAlerta.ABERTO);
    }

    @Test
    @DisplayName("se alguém do time já começou o ciclo, a troca vale só a partir do próximo")
    void trocaNoProximoCiclo() throws Exception {
        Alerta a = depoisDoCiclo4();
        Long atendimento = timeId(PessoasDemo.TIME_ATENDIMENTO);

        // Alguém do Atendimento abre a 5A antes da decisão da gestão.
        Usuario bruno = usuarios.findByEmail(PessoasDemo.email("Bruno Tavares")).orElseThrow();
        Long cinco = ciclos.cicloAberto().orElseThrow().getId();
        Long atividade5A = liberadas.escopoDoTime(atendimento, cinco).getFirst().getId();
        execucao.abrir(new UsuarioAutenticado(bruno), atividade5A);

        mockMvc.perform(post("/gestao/alertas/{id}/recomendacao/aceitar", a.getId()).with(renata()).with(csrf()));
        assertThat(html("/gestao/times/" + atendimento + "/escopo")).contains("a troca vale a partir do ciclo 6");
        mockMvc.perform(post("/gestao/times/{id}/escopo", atendimento).with(renata()).with(csrf())
                .param("alertaId", a.getId().toString()).param("aplicarTroca", "true"));

        assertThat(escopoDoAtendimentoNoCicloAberto()).containsExactlyInAnyOrder("5A", "5B");
        assertThat(recomendacoes.doAlerta(a.getId())).allMatch(r -> r.getCicloAplicacao() == null);

        gerador.gerarCicloAberto();
        fechamento.fecharCicloAberto();

        assertThat(escopoDoAtendimentoNoCicloAberto()).containsExactlyInAnyOrder("TRILHA-VERIFICACAO", "6B");
        assertThat(recomendacoes.doAlerta(a.getId()))
                .allMatch(r -> r.getCicloAplicacao() != null && r.getCicloAplicacao().getNumero() == 6);
    }

    @Test
    @DisplayName("privacidade: sem amostra mínima, o detalhe não mostra dificuldades nem confiança")
    void privacidadeSemAmostra() throws Exception {
        // Ciclo 1 vazio: só 4 pessoas do Marketing respondem, marcando dificuldades.
        Long marketing = timeId(PessoasDemo.TIME_MARKETING);
        List<Usuario> membros = usuarios.findByTimeIdOrderByNome(marketing);
        Long receio = dificuldades.findAllByOrderById().getFirst().getId();
        for (int i = 0; i < 4; i++) {
            checkins.registrar(membros.get(i).getId(), new RespostaCheckin(2, 5, List.of(receio), false));
        }
        fechamento.fecharCicloAberto();

        String pagina = html("/gestao/times/" + marketing);

        assertThat(pagina).contains("Amostra insuficiente: 4 de 8 check-ins, mínimo de 5");
        assertThat(pagina).doesNotContain("Receio de substituição");
        assertThat(html("/gestao")).contains("amostra insuficiente");
    }
}
