package br.com.prontidaoia.usuario;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.checkin.CheckinRepository;
import br.com.prontidaoia.atividade.TentativaRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD do proprio colaborador; gestor nao pode ser criado por formulario. */
@Service
public class ContaColaboradorService {
    private static final Set<String> DEPARTAMENTOS = Set.of("Atendimento", "Financeiro", "Marketing");
    private final UsuarioRepository usuarios;
    private final TimeRepository times;
    private final PasswordEncoder encoder;
    private final CheckinRepository checkins;
    private final TentativaRepository tentativas;

    public record DadosPerfil(Long id, String nome, String email, String departamento,
                              Long departamentoId, boolean editavel) {}

    public ContaColaboradorService(UsuarioRepository usuarios, TimeRepository times, PasswordEncoder encoder,
                                   CheckinRepository checkins, TentativaRepository tentativas) {
        this.usuarios = usuarios;
        this.times = times;
        this.encoder = encoder;
        this.checkins = checkins;
        this.tentativas = tentativas;
    }

    @Transactional(readOnly = true)
    public List<TimeEntity> departamentos() {
        return times.findAllByOrderByNome().stream()
                .filter(t -> DEPARTAMENTOS.contains(t.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DadosPerfil perfil(Long usuarioId) {
        Usuario usuario = buscarAtivo(usuarioId);
        if (usuario.getPapel() != Papel.COLABORADOR) {
            throw new RegraVioladaException("O perfil de colaborador não está disponível para gestores.");
        }
        return new DadosPerfil(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getTime().getNome(), usuario.getTime().getId(), usuario.isAutocadastro());
    }

    @Transactional
    public void cadastrar(CadastroForm formulario) {
        String nome = nome(formulario.getNome());
        String email = email(formulario.getEmail());
        validarSenha(formulario.getSenha(), formulario.getConfirmacao(), true);
        TimeEntity time = departamento(formulario.getDepartamentoId());
        if (usuarios.existsByEmail(email)) {
            throw new RegraVioladaException("Este e-mail já está cadastrado.");
        }
        Usuario pessoa = Usuario.novoColaborador(nome, email, encoder.encode(formulario.getSenha()), time);
        try {
            usuarios.saveAndFlush(pessoa);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraVioladaException("Este e-mail já está cadastrado.");
        }
    }

    @Transactional
    public void atualizar(Long usuarioId, PerfilForm formulario) {
        Usuario pessoa = buscarEditavel(usuarioId);
        if (!encoder.matches(formulario.getSenhaAtual(), pessoa.getSenhaHash())) {
            throw new RegraVioladaException("Senha atual incorreta.");
        }
        String nome = nome(formulario.getNome());
        String email = email(formulario.getEmail());
        TimeEntity time = departamento(formulario.getDepartamentoId());
        if (!time.getId().equals(pessoa.getTime().getId())
                && (checkins.existsByUsuarioId(usuarioId) || tentativas.existsByUsuarioId(usuarioId))) {
            throw new RegraVioladaException("Após participar de check-ins ou atividades, o departamento não pode ser alterado: isso mudaria o histórico dos indicadores.");
        }
        String novaSenha = formulario.getNovaSenha();
        boolean trocarSenha = novaSenha != null && !novaSenha.isBlank();
        if (trocarSenha || (formulario.getConfirmacaoNovaSenha() != null
                && !formulario.getConfirmacaoNovaSenha().isBlank())) {
            validarSenha(novaSenha, formulario.getConfirmacaoNovaSenha(), true);
        }
        if (!pessoa.getEmail().equals(email) && usuarios.existsByEmail(email)) {
            throw new RegraVioladaException("Este e-mail já está cadastrado.");
        }
        try {
            pessoa.atualizarCadastro(nome, email, time, trocarSenha ? encoder.encode(novaSenha) : null);
            usuarios.saveAndFlush(pessoa);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraVioladaException("Este e-mail já está cadastrado.");
        }
    }

    @Transactional
    public void excluir(Long usuarioId, String senha, String confirmacao) {
        Usuario pessoa = buscarEditavel(usuarioId);
        if (!"EXCLUIR".equals(confirmacao)) {
            throw new RegraVioladaException("Digite EXCLUIR para confirmar.");
        }
        if (!encoder.matches(senha, pessoa.getSenhaHash())) {
            throw new RegraVioladaException("Senha incorreta. Conta não excluída.");
        }
        // Nenhum DELETE na tabela usuario: as FKs de check-in e tentativa permanecem validas.
        pessoa.excluirCadastro(encoder.encode(UUID.randomUUID().toString()));
        usuarios.saveAndFlush(pessoa);
    }

    private Usuario buscarAtivo(Long id) {
        return usuarios.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RegraVioladaException("Conta não encontrada ou desativada."));
    }

    private Usuario buscarEditavel(Long id) {
        Usuario pessoa = buscarAtivo(id);
        if (pessoa.getPapel() != Papel.COLABORADOR || !pessoa.isAutocadastro()) {
            throw new RegraVioladaException("Contas pré-configuradas são protegidas para preservar a demonstração.");
        }
        return pessoa;
    }

    private TimeEntity departamento(Long id) {
        if (id == null) throw new RegraVioladaException("Selecione um departamento.");
        TimeEntity time = times.findById(id)
                .orElseThrow(() -> new RegraVioladaException("Departamento inválido."));
        if (!DEPARTAMENTOS.contains(time.getNome())) {
            throw new RegraVioladaException("Departamento inválido.");
        }
        return time;
    }

    private String nome(String valor) {
        String n = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        if (n.isBlank() || n.length() > 120) {
            throw new RegraVioladaException("Informe um nome de até 120 caracteres.");
        }
        return n;
    }

    private String email(String valor) {
        String e = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        // @Email na camada web; defesa adicional na camada de dominio.
        if (e.length() > 160 || !e.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RegraVioladaException("Informe um e-mail válido.");
        }
        return e;
    }

    private void validarSenha(String senha, String confirmacao, boolean obrigatoria) {
        if (obrigatoria && (senha == null || senha.length() < 8 || senha.length() > 72)) {
            throw new RegraVioladaException("A senha deve ter entre 8 e 72 caracteres.");
        }
        if (senha == null || !senha.equals(confirmacao)) {
            throw new RegraVioladaException("As senhas não coincidem.");
        }
    }
}
