package br.com.prontidaoia.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.usuario.UsuarioDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Separação dos dois papéis (seção 2) e primeira camada da regra de
 * privacidade da seção 4: o gestor não alcança nenhuma rota de colaborador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DisplayName("Segurança das rotas")
class SegurancaWebTest extends TesteComBanco {

    private static final String EMAIL_CARLA = PessoasDemo.email(PessoasDemo.CARLA);
    private static final String EMAIL_RENATA = PessoasDemo.email(PessoasDemo.GESTORA);
    private static final String SENHA = "demo123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioDetailsService usuarios;

    /**
     * Entra com o usuário carregado do banco, como no login de verdade: os
     * controllers recebem um {@code UsuarioAutenticado}, não um usuário genérico.
     */
    private RequestPostProcessor como(String email) {
        return user(usuarios.loadUserByUsername(email));
    }

    @Test
    @DisplayName("a tela de login é pública")
    void loginEhPublico() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("quem não entrou é mandado para o login")
    void anonimoVaiParaOLogin() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("a colaboradora entra com e-mail e senha")
    void colaboradoraEntra() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", EMAIL_CARLA)
                        .password("senha", SENHA))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("senha errada não autentica")
    void senhaErradaNaoEntra() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", EMAIL_CARLA)
                        .password("senha", "senha-errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    @DisplayName("a raiz leva a colaboradora ao início e a gestora à gestão")
    void raizRedirecionaPorPapel() throws Exception {
        mockMvc.perform(get("/").with(como(EMAIL_CARLA)))
                .andExpect(redirectedUrl("/inicio"));

        mockMvc.perform(get("/").with(como(EMAIL_RENATA)))
                .andExpect(redirectedUrl("/gestao"));
    }

    @Test
    @DisplayName("a colaboradora abre /inicio e não abre /gestao")
    void colaboradoraNaoEntraNaGestao() throws Exception {
        mockMvc.perform(get("/inicio").with(como(EMAIL_CARLA)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/gestao").with(como(EMAIL_CARLA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a gestora abre /gestao e não abre as telas do colaborador")
    void gestoraNaoEntraNasTelasDoColaborador() throws Exception {
        mockMvc.perform(get("/gestao").with(como(EMAIL_RENATA)))
                .andExpect(status().isOk());

        for (String rotaDoColaborador : new String[]{"/inicio", "/checkin", "/meu-historico"}) {
            mockMvc.perform(get(rotaDoColaborador).with(como(EMAIL_RENATA)))
                    .andExpect(status().isForbidden());
        }
    }
}
