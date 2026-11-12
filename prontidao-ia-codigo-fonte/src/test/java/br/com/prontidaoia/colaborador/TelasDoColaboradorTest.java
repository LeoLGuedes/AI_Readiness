package br.com.prontidaoia.colaborador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.Questao;
import br.com.prontidaoia.atividade.QuestaoRepository;
import br.com.prontidaoia.checkin.CheckinService;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.checkin.RespostaCheckin;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.colaborador.HistoricoPessoalService.LinhaHistorico;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioDetailsService;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Percorre as telas do colaborador como a Carla fará na demonstração
 * (seção 9, passo 2) e confere a privacidade do histórico pessoal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
@DisplayName("Telas do colaborador")
class TelasDoColaboradorTest extends TesteComBanco {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioDetailsService detalhes;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private DificuldadeRepository dificuldades;

    @Autowired
    private AtividadeLiberadaRepository liberadas;

    @Autowired
    private QuestaoRepository questoes;

    @Autowired
    private CicloService ciclos;

    @Autowired
    private CheckinService checkins;

    private Usuario carla;
    private Usuario bruno;

    @BeforeEach
    void preparar() {
        carla = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.CARLA)).orElseThrow();
        bruno = usuarios.findByEmail(PessoasDemo.email("Bruno Tavares")).orElseThrow();
    }

    private RequestPostProcessor como(Usuario pessoa) {
        return user(detalhes.loadUserByUsername(pessoa.getEmail()));
    }

    private AtividadeLiberada primeiraAtividadeDaCarla() {
        Long ciclo = ciclos.cicloAberto().orElseThrow().getId();
        return liberadas.escopoDoTime(carla.getTime().getId(), ciclo).getFirst();
    }

    @Test
    @DisplayName("o início mostra o ciclo e as três pendências")
    void inicioMostraPendencias() throws Exception {
        MvcResult resposta = mockMvc.perform(get("/inicio").with(como(carla)))
                .andExpect(status().isOk())
                .andReturn();

        String html = resposta.getResponse().getContentAsString();
        assertThat(html).contains("Ciclo 1 de 6", "Check-in semanal", "Atividade 1A", "Atividade 1B",
                "0 de 3 pendências concluídas");
    }

    @Test
    @DisplayName("o check-in enviado volta ao início com a confirmação; o segundo é recusado")
    void checkinPelaTela() throws Exception {
        mockMvc.perform(get("/checkin").with(como(carla)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Sua gestora vê apenas resultados do time, nunca as suas respostas")));

        String receio = dificuldades.findAllByOrderById().getFirst().getId().toString();
        MockHttpServletRequestBuilder envio = post("/checkin").with(como(carla)).with(csrf())
                .param("confianca", "2").param("receio", "4").param("dificuldades", receio);

        mockMvc.perform(envio)
                .andExpect(redirectedUrl("/inicio"))
                .andExpect(flash().attributeExists("sucesso"));

        mockMvc.perform(post("/checkin").with(como(carla)).with(csrf())
                        .param("confianca", "5").param("receio", "1").param("nenhuma", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Você já fez o check-in deste ciclo.")));
    }

    @Test
    @DisplayName("check-in sem resposta volta ao formulário com os erros no campo")
    void checkinIncompleto() throws Exception {
        mockMvc.perform(post("/checkin").with(como(carla)).with(csrf()).param("receio", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "confianca", "pergunta3Respondida"));
    }

    @Test
    @DisplayName("a atividade abre, finaliza e mostra o resultado com o gabarito")
    void atividadeDePontaAPonta() throws Exception {
        AtividadeLiberada atividade = primeiraAtividadeDaCarla();
        List<Questao> dez = questoes.daAtividade(atividade.getAtividade().getId());

        mockMvc.perform(get("/atividades/{id}", atividade.getId()).with(como(carla)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Finalizar atividade")));

        MockHttpServletRequestBuilder envio = post("/atividades/{id}/finalizar", atividade.getId())
                .with(como(carla)).with(csrf());
        for (Questao q : dez) {
            envio.param("q" + q.getId(), String.valueOf(q.getIndiceCorreto()))
                 .param("t" + q.getId(), "45");
        }
        mockMvc.perform(envio)
                .andExpect(redirectedUrl("/atividades/" + atividade.getId() + "/resultado"));

        mockMvc.perform(get("/atividades/{id}/resultado", atividade.getId()).with(como(carla)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10 de 10")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gabarito comentado")));

        // Abrir de novo uma atividade finalizada leva ao resultado, não a uma nova tentativa.
        mockMvc.perform(get("/atividades/{id}", atividade.getId()).with(como(carla)))
                .andExpect(redirectedUrl("/atividades/" + atividade.getId() + "/resultado"));
    }

    @Test
    @DisplayName("finalizar com questão em branco volta à atividade com a mensagem")
    void atividadeComQuestaoEmBranco() throws Exception {
        AtividadeLiberada atividade = primeiraAtividadeDaCarla();
        mockMvc.perform(get("/atividades/{id}", atividade.getId()).with(como(carla)));

        mockMvc.perform(post("/atividades/{id}/finalizar", atividade.getId()).with(como(carla)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Responda todas as questões antes de finalizar")));
    }

    @Test
    @DisplayName("a atividade de outro time responde 404")
    void atividadeDeOutroTime() throws Exception {
        Usuario doFinanceiro = usuarios.findByEmail(PessoasDemo.email("Eduardo Salgado")).orElseThrow();

        mockMvc.perform(get("/atividades/{id}", primeiraAtividadeDaCarla().getId()).with(como(doFinanceiro)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/meu-historico devolve só os dados de quem está logado, mesmo com o id de outra pessoa na URL")
    void historicoSoDaPropriaPessoa() throws Exception {
        Long receio = dificuldades.findAllByOrderById().getFirst().getId();
        checkins.registrar(carla.getId(), new RespostaCheckin(2, 4, List.of(receio), false));
        checkins.registrar(bruno.getId(), new RespostaCheckin(5, 1, List.of(), true));

        MvcResult resposta = mockMvc.perform(get("/meu-historico")
                        .param("usuarioId", bruno.getId().toString())
                        .param("usuario", bruno.getEmail())
                        .with(como(carla)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Só você vê estes dados.")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<LinhaHistorico> linhas = (List<LinhaHistorico>) resposta.getModelAndView().getModel().get("linhas");
        assertThat(linhas).hasSize(1);
        assertThat(linhas.getFirst().confianca()).isEqualTo(2);
        assertThat(linhas.getFirst().receio()).isEqualTo(4);
        assertThat(linhas.getFirst().confiancaPercentual()).isEqualTo(25);
    }

    @Test
    @DisplayName("o gestor não abre nenhuma tela do colaborador")
    void gestorNaoAbreTelasDoColaborador() throws Exception {
        Usuario renata = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.GESTORA)).orElseThrow();
        AtividadeLiberada atividade = primeiraAtividadeDaCarla();

        for (String rota : List.of("/inicio", "/checkin", "/meu-historico",
                "/atividades/" + atividade.getId(), "/atividades/" + atividade.getId() + "/resultado")) {
            mockMvc.perform(get(rota).with(como(renata))).andExpect(status().isForbidden());
        }
    }
}
