package br.com.prontidaoia.pbl7;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.simulacao.CenarioInicial;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.usuario.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** PBL 7: caracterização da segurança HTTP com MockMvc e banco isolado de teste. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
@DisplayName("PBL7 - controle de acesso e CSRF")
class AvaliacaoSegurancaPbl7Test extends TesteComBanco {

    @Autowired private MockMvc mvc;
    @Autowired private CenarioInicial cenario;
    @Autowired private UsuarioDetailsService detalhes;

    @BeforeEach
    void preparar() {
        cenario.restaurar();
    }

    private RequestPostProcessor como(String nome) {
        return user(detalhes.loadUserByUsername(PessoasDemo.email(nome)));
    }

    @Test
    @DisplayName("ações de restauração, geração e fechamento recusam POST sem CSRF")
    void acoesDemoExigemCsrf() throws Exception {
        RequestPostProcessor renata = como(PessoasDemo.GESTORA);
        for (String rota : new String[]{
                "/demo/restaurar", "/demo/carla", "/demo/gerar-respostas",
                "/demo/fechar-ciclo", "/demo/avancar-ciclo"}) {
            mvc.perform(post(rota).with(renata))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("colaboradora não pode disparar ação da gestão, mesmo com CSRF válido")
    void colaboradoraNaoAlteraGestao() throws Exception {
        mvc.perform(post("/gestao/alertas/1/recomendacao/adiar")
                        .with(como(PessoasDemo.CARLA)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("gestora não envia check-in de colaborador, mesmo com CSRF válido")
    void gestoraNaoEnviaCheckin() throws Exception {
        mvc.perform(post("/checkin").with(como(PessoasDemo.GESTORA)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("painel /demo é compartilhado com colaboradores (limitação atual do perfil demo)")
    void painelDemoEhCompartilhadoNoPerfilDemo() throws Exception {
        mvc.perform(get("/demo").with(como(PessoasDemo.CARLA)))
                .andExpect(status().isOk());
        // Esse comportamento é deliberado no SecurityConfig atual.
        // O teste o documenta: NÃO significa que é seguro para produção.
    }
}
