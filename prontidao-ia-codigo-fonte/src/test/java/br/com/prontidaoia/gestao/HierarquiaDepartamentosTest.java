package br.com.prontidaoia.gestao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.fechamento.FechamentoCicloService;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.simulacao.CenarioInicial;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.simulacao.PreenchimentoCarla;
import br.com.prontidaoia.usuario.Papel;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import br.com.prontidaoia.usuario.UsuarioDetailsService;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hierarquia de departamentos: a gerente de RH vê todos os times; cada líder
 * vê e cria colaboradores somente no próprio departamento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
@DisplayName("Hierarquia de departamentos: RH, líderes e colaboradores")
class HierarquiaDepartamentosTest extends TesteComBanco {

    private static final String SENHA = "senha-forte-123";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioDetailsService detalhes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TimeRepository times;
    @Autowired private CenarioInicial cenario;
    @Autowired private PreenchimentoCarla carla;
    @Autowired private FechamentoCicloService fechamento;
    @Autowired private AlertaRepository alertas;
    @Autowired private RecomendacaoRepository recomendacoes;

    private RequestPostProcessor como(String nome) {
        return user(detalhes.loadUserByUsername(PessoasDemo.email(nome)));
    }

    private TimeEntity time(String nome) {
        return times.findByNome(nome).orElseThrow();
    }

    private String html(String rota, String quem) throws Exception {
        return mvc.perform(get(rota).with(como(quem)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Cenário restaurado, Carla preenchida e ciclo 4 fechado: há alertas no Atendimento e no Marketing. */
    private void depoisDoCiclo4() {
        cenario.restaurar();
        carla.preencher();
        fechamento.fecharCicloAberto();
    }

    private Alerta alertaAberto(String departamento) {
        return alertas.findByTimeIdOrderById(time(departamento).getId()).stream()
                .filter(a -> a.getStatus() == StatusAlerta.ABERTO)
                .findFirst().orElseThrow();
    }

    private ResultActions criar(String quem, String departamento, String email) throws Exception {
        return mvc.perform(post("/gestao/equipe").with(csrf()).with(como(quem))
                .param("nome", "Pessoa Nova").param("email", email)
                .param("departamentoId", time(departamento).getId().toString())
                .param("senha", SENHA).param("confirmacao", SENHA)
                .param("papel", "GESTOR")); // ignorado: o papel nunca vem do formulário
    }

    private String emailNovo() {
        return "novo-" + UUID.randomUUID() + "@example.org";
    }

    // ------------------------------------------------------------------
    // Estrutura
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cada departamento tem um líder, que não é contado como membro do time")
    void umLiderPorDepartamento() {
        assertThat(time(PessoasDemo.TIME_ATENDIMENTO).getLider().getNome()).isEqualTo(PessoasDemo.LIDER_ATENDIMENTO);
        assertThat(time(PessoasDemo.TIME_FINANCEIRO).getLider().getNome()).isEqualTo(PessoasDemo.LIDER_FINANCEIRO);
        assertThat(time(PessoasDemo.TIME_MARKETING).getLider().getNome()).isEqualTo(PessoasDemo.LIDER_MARKETING);

        assertThat(usuarios.findByPapelOrderByNome(Papel.LIDER)).hasSize(3)
                .allSatisfy(l -> assertThat(l.getTime()).isNull());
        times.findAllByOrderByNome().forEach(t ->
                assertThat(usuarios.countByTimeId(t.getId())).as("membros de %s", t.getNome()).isEqualTo(8));
    }

    @Test
    @DisplayName("a sessão do líder traz o departamento que ele lidera, lido do banco")
    void sessaoDoLider() {
        UsuarioAutenticado ricardo = (UsuarioAutenticado) detalhes.loadUserByUsername(
                PessoasDemo.email(PessoasDemo.LIDER_FINANCEIRO));

        assertThat(ricardo.isLider()).isTrue();
        assertThat(ricardo.isAcessaGestao()).isTrue();
        assertThat(ricardo.getDepartamentoLideradoId()).isEqualTo(time(PessoasDemo.TIME_FINANCEIRO).getId());
        assertThat(ricardo.getRotuloPapel()).isEqualTo("Líder · Financeiro");
    }

    // ------------------------------------------------------------------
    // Visualização
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a gerente de RH vê os três departamentos na visão geral")
    void rhVeTodos() throws Exception {
        depoisDoCiclo4();

        String pagina = html("/gestao", PessoasDemo.GESTORA);

        assertThat(pagina).contains("Visão geral dos times", "Maior erro da empresa",
                ">Atendimento<", ">Financeiro<", ">Marketing<");
        for (TimeEntity t : times.findAllByOrderByNome()) {
            html("/gestao/times/" + t.getId(), PessoasDemo.GESTORA);
        }
    }

    @Test
    @DisplayName("o líder vê na visão geral somente o próprio departamento")
    void liderVeSoOProprio() throws Exception {
        depoisDoCiclo4();

        String pagina = html("/gestao", PessoasDemo.LIDER_FINANCEIRO);

        assertThat(pagina).contains("Visão geral do departamento", ">Financeiro<", "Maior erro do departamento");
        assertThat(pagina).doesNotContain(">Atendimento<", ">Marketing<", "Maior erro da empresa",
                "Sabe usar, mas não se sente pronto", "Confiança alta sem senso crítico");
    }

    @Test
    @DisplayName("o líder abre o detalhe do próprio time e recebe 403 nos demais")
    void liderNaoAbreOutroTime() throws Exception {
        depoisDoCiclo4();
        Long financeiro = time(PessoasDemo.TIME_FINANCEIRO).getId();

        html("/gestao/times/" + financeiro, PessoasDemo.LIDER_FINANCEIRO);
        html("/gestao/times/" + financeiro + "/escopo", PessoasDemo.LIDER_FINANCEIRO);

        for (String outro : new String[]{PessoasDemo.TIME_ATENDIMENTO, PessoasDemo.TIME_MARKETING}) {
            Long id = time(outro).getId();
            mvc.perform(get("/gestao/times/" + id).with(como(PessoasDemo.LIDER_FINANCEIRO)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/gestao/times/" + id + "/escopo").with(como(PessoasDemo.LIDER_FINANCEIRO)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("o líder decide sobre o alerta do próprio time, mas não sobre o de outro departamento")
    void liderSoDecideNoProprioTime() throws Exception {
        depoisDoCiclo4();
        Alerta doAtendimento = alertaAberto(PessoasDemo.TIME_ATENDIMENTO);

        // Líder do Marketing tentando ver e adiar o alerta do Atendimento.
        mvc.perform(get("/gestao/alertas/" + doAtendimento.getId() + "/recomendacao")
                        .with(como(PessoasDemo.LIDER_MARKETING)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/gestao/alertas/" + doAtendimento.getId() + "/recomendacao/adiar")
                        .with(csrf()).with(como(PessoasDemo.LIDER_MARKETING)))
                .andExpect(status().isForbidden());
        Long recomendacao = recomendacoes.doAlerta(doAtendimento.getId()).getFirst().getId();
        mvc.perform(post("/gestao/recomendacoes/" + recomendacao + "/agendar")
                        .with(csrf()).with(como(PessoasDemo.LIDER_MARKETING)).param("data", "2030-01-01"))
                .andExpect(status().isForbidden());
        assertThat(alertas.findById(doAtendimento.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusAlerta.ABERTO);

        // Escopo: alerta de outro time enviado pela URL do próprio time.
        Long marketing = time(PessoasDemo.TIME_MARKETING).getId();
        mvc.perform(post("/gestao/times/" + marketing + "/escopo").with(csrf())
                        .with(como(PessoasDemo.LIDER_MARKETING))
                        .param("alertaId", doAtendimento.getId().toString()))
                .andExpect(status().isForbidden());

        // A líder do Atendimento abre a recomendação do próprio time.
        String pagina = html("/gestao/alertas/" + doAtendimento.getId() + "/recomendacao",
                PessoasDemo.LIDER_ATENDIMENTO);
        assertThat(pagina).contains("Atendimento");
    }

    @Test
    @DisplayName("a raiz leva o líder para a gestão, e ele não abre as telas do colaborador")
    void liderNaGestao() throws Exception {
        mvc.perform(get("/").with(como(PessoasDemo.LIDER_MARKETING)))
                .andExpect(redirectedUrl("/gestao"));
        for (String rota : new String[]{"/inicio", "/checkin", "/meu-historico", "/perfil"}) {
            mvc.perform(get(rota).with(como(PessoasDemo.LIDER_MARKETING))).andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------
    // Criação de colaboradores
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o líder cria colaborador no próprio departamento, sempre com papel COLABORADOR")
    void liderCriaNoProprioDepartamento() throws Exception {
        String email = emailNovo();

        criar(PessoasDemo.LIDER_FINANCEIRO, PessoasDemo.TIME_FINANCEIRO, email)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestao/equipe"));

        Usuario criado = usuarios.findByEmail(email).orElseThrow();
        assertThat(criado.getPapel()).isEqualTo(Papel.COLABORADOR);
        assertThat(criado.getTime().getNome()).isEqualTo(PessoasDemo.TIME_FINANCEIRO);
        assertThat(criado.getCriadoPor().getNome()).isEqualTo(PessoasDemo.LIDER_FINANCEIRO);
        assertThat(criado.isAtivo()).isTrue();
        // A nova pessoa entra como colaboradora.
        mvc.perform(get("/inicio").with(user(detalhes.loadUserByUsername(email)))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("o líder NÃO cria colaborador em outro departamento, mesmo alterando o formulário")
    void liderNaoCriaEmOutroDepartamento() throws Exception {
        String email = emailNovo();

        String pagina = criar(PessoasDemo.LIDER_FINANCEIRO, PessoasDemo.TIME_MARKETING, email)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(pagina).contains("você só pode criar colaboradores para o Financeiro");
        assertThat(usuarios.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("a gerente de RH cria colaborador em qualquer departamento")
    void rhCriaEmQualquerDepartamento() throws Exception {
        for (String departamento : new String[]{
                PessoasDemo.TIME_ATENDIMENTO, PessoasDemo.TIME_FINANCEIRO, PessoasDemo.TIME_MARKETING}) {
            String email = emailNovo();
            criar(PessoasDemo.GESTORA, departamento, email).andExpect(status().is3xxRedirection());
            Usuario criado = usuarios.findByEmail(email).orElseThrow();
            assertThat(criado.getTime().getNome()).isEqualTo(departamento);
            assertThat(criado.getPapel()).isEqualTo(Papel.COLABORADOR);
        }
    }

    @Test
    @DisplayName("colaborador não abre a tela de equipe nem cria contas")
    void colaboradorNaoCria() throws Exception {
        String email = emailNovo();
        mvc.perform(get("/gestao/equipe").with(como(PessoasDemo.CARLA))).andExpect(status().isForbidden());
        criar(PessoasDemo.CARLA, PessoasDemo.TIME_ATENDIMENTO, email).andExpect(status().isForbidden());
        assertThat(usuarios.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("a tela de equipe do líder mostra só o próprio departamento, sem nomes de colaboradores")
    void telaDeEquipe() throws Exception {
        String lider = html("/gestao/equipe", PessoasDemo.LIDER_MARKETING);
        assertThat(lider).contains("Equipe · Marketing", "Líder: " + PessoasDemo.LIDER_MARKETING)
                .doesNotContain(">Atendimento<", ">Financeiro<");

        String rh = html("/gestao/equipe", PessoasDemo.GESTORA);
        assertThat(rh).contains(">Atendimento<", ">Financeiro<", ">Marketing<");

        // Privacidade (seção 4): nenhuma tela da gestão lista as pessoas.
        usuarios.findByPapelOrderByNome(Papel.COLABORADOR).forEach(c -> {
            assertThat(lider).doesNotContain(c.getNome());
            assertThat(rh).doesNotContain(c.getNome());
        });
    }
}
