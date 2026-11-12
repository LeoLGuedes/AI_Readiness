package br.com.prontidaoia.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.simulacao.PessoasDemo;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
@DisplayName("CRUD de colaborador: cadastro, perfil e exclusão segura")
class CadastroPerfilCrudTest extends TesteComBanco {
    private static final String SENHA = "senha-forte-123";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private UsuarioDetailsService autenticacao;
    @Autowired private TimeRepository times;
    @Autowired private ContaColaboradorService contas;
    @Autowired private PasswordEncoder encoder;

    private String emailNovo() { return "aluno-" + UUID.randomUUID() + "@example.org"; }
    private TimeEntity time(String nome) { return times.findByNome(nome).orElseThrow(); }

    private void registrar(String email, String nome, String departamento) throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", nome).param("email", email).param("senha", SENHA)
                        .param("confirmacao", SENHA)
                        .param("departamentoId", time(departamento).getId().toString())
                        .param("papel", "GESTOR")) // ignorado mesmo quando enviado maliciosamente
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?criado"));
    }

    @Test
    @DisplayName("cadastro público cria SOMENTE colaborador, com departamento e hash BCrypt")
    void cadastroNovoColaborador() throws Exception {
        String email = emailNovo();
        mvc.perform(get("/cadastro")).andExpect(status().isOk());
        registrar(email, "Nova Pessoa", "Atendimento");
        Usuario u = usuarios.findByEmail(email).orElseThrow();
        assertThat(u.getPapel()).isEqualTo(Papel.COLABORADOR);
        assertThat(u.getTime().getNome()).isEqualTo("Atendimento");
        assertThat(u.isAutocadastro()).isTrue();
        assertThat(u.isAtivo()).isTrue();
        assertThat(encoder.matches(SENHA, u.getSenhaHash())).isTrue();
        mvc.perform(get("/perfil").with(user(autenticacao.loadUserByUsername(email))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cadastro recusa CSRF ausente e e-mail duplicado")
    void csrfEDuplicado() throws Exception {
        String email = emailNovo();
        mvc.perform(post("/cadastro").param("email", email)).andExpect(status().isForbidden());
        registrar(email, "Primeira Pessoa", "Financeiro");
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Outra Pessoa").param("email", email.toUpperCase())
                        .param("senha", SENHA).param("confirmacao", SENHA)
                        .param("departamentoId", time("Financeiro").getId().toString()))
                .andExpect(status().isOk());
        assertThat(usuarios.findByEmail(email)).isPresent();
    }

    @Test
    @DisplayName("cadastro recusa departamento inexistente e senhas diferentes")
    void validaFormulario() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Teste").param("email", emailNovo())
                        .param("senha", SENHA).param("confirmacao", "outro")
                        .param("departamentoId", "999999"))
                .andExpect(status().isOk());
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Teste").param("email", emailNovo())
                        .param("senha", SENHA).param("confirmacao", SENHA)
                        .param("departamentoId", "999999"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("o próprio colaborador pode alterar nome, e-mail, departamento e senha")
    void atualizaPerfil() throws Exception {
        String email = emailNovo();
        registrar(email, "Nome Antigo", "Atendimento");
        Usuario u = usuarios.findByEmail(email).orElseThrow();
        String emailNovo = emailNovo();
        mvc.perform(post("/perfil").with(csrf()).with(user(autenticacao.loadUserByUsername(email)))
                        .param("nome", "Nome Atualizado").param("email", emailNovo)
                        .param("departamentoId", time("Marketing").getId().toString())
                        .param("senhaAtual", SENHA).param("novaSenha", "outra-senha-123")
                        .param("confirmacaoNovaSenha", "outra-senha-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?atualizado"));
        Usuario atualizado = usuarios.findById(u.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Nome Atualizado");
        assertThat(atualizado.getEmail()).isEqualTo(emailNovo);
        assertThat(atualizado.getTime().getNome()).isEqualTo("Marketing");
        assertThat(encoder.matches("outra-senha-123", atualizado.getSenhaHash())).isTrue();
        assertThat(atualizado.getPapel()).isEqualTo(Papel.COLABORADOR);
    }

    @Test
    @DisplayName("exclusão impede novo login e preserva o registro associado ao histórico")
    void excluiCadastroSemApagarHistorico() throws Exception {
        String email = emailNovo();
        registrar(email, "Pessoa Para Excluir", "Financeiro");
        Usuario u = usuarios.findByEmail(email).orElseThrow();
        Long id = u.getId();
        mvc.perform(post("/perfil/excluir").with(csrf()).with(user(autenticacao.loadUserByUsername(email)))
                        .param("senhaExclusao", SENHA).param("confirmacaoExclusao", "EXCLUIR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?excluido"));
        Usuario excluido = usuarios.findById(id).orElseThrow();
        assertThat(excluido.isAtivo()).isFalse();
        assertThat(excluido.getNome()).isEqualTo("Colaborador excluído");
        assertThat(excluido.getEmail()).doesNotContain(email);
        assertThat(excluido.getTime().getNome()).isEqualTo("Financeiro");
    }

    @Test
    @DisplayName("contas pré-configuradas permanecem protegidas e gestor não acessa perfil")
    void protegeDemoEGestao() throws Exception {
        String carla = PessoasDemo.email(PessoasDemo.CARLA);
        String renata = PessoasDemo.email(PessoasDemo.GESTORA);
        mvc.perform(get("/perfil").with(user(autenticacao.loadUserByUsername(carla))))
                .andExpect(status().isOk());
        mvc.perform(get("/perfil").with(user(autenticacao.loadUserByUsername(renata))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/perfil/excluir").with(csrf())
                        .with(user(autenticacao.loadUserByUsername(carla)))
                        .param("senhaExclusao", "demo123").param("confirmacaoExclusao", "EXCLUIR"))
                .andExpect(status().isOk());
        assertThat(usuarios.findByEmail(carla).orElseThrow().isAtivo()).isTrue();
    }
}
