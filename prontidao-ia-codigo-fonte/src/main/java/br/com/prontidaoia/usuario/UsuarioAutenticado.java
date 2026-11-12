package br.com.prontidaoia.usuario;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Identidade da sessao.
 *
 * <p>Carrega os dados ja resolvidos (id, time, nome) para que nenhuma tela
 * precise voltar ao banco so para saber quem esta logado, e para que o
 * {@code timeId} usado nas consultas venha da sessao, nunca da URL.</p>
 */
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String nome;
    private final String iniciais;
    private final String email;
    private final String senhaHash;
    private final Papel papel;
    private final boolean ativo;
    private final Long timeId;
    private final String nomeDoTime;
    /** Departamento liderado; preenchido somente para o papel LIDER. */
    private final Long departamentoLideradoId;
    private final String nomeDoDepartamentoLiderado;

    public UsuarioAutenticado(Usuario usuario) {
        this(usuario, null);
    }

    /**
     * @param departamentoLiderado o time que o usuario lidera (papel LIDER);
     *                             nulo para os demais papeis
     */
    public UsuarioAutenticado(Usuario usuario, TimeEntity departamentoLiderado) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.iniciais = usuario.getIniciais();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.papel = usuario.getPapel();
        this.ativo = usuario.isAtivo();
        this.timeId = usuario.getTime() == null ? null : usuario.getTime().getId();
        this.nomeDoTime = usuario.getTime() == null ? null : usuario.getTime().getNome();
        boolean lidera = usuario.isLider() && departamentoLiderado != null;
        this.departamentoLideradoId = lidera ? departamentoLiderado.getId() : null;
        this.nomeDoDepartamentoLiderado = lidera ? departamentoLiderado.getNome() : null;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getPrimeiroNome() {
        int espaco = nome.indexOf(' ');
        return espaco < 0 ? nome : nome.substring(0, espaco);
    }

    public String getIniciais() {
        return iniciais;
    }

    public Papel getPapel() {
        return papel;
    }

    /** Nulo para o gestor. */
    public Long getTimeId() {
        return timeId;
    }

    public String getNomeDoTime() {
        return nomeDoTime;
    }

    /** Gerente de RH: ve todos os departamentos. */
    public boolean isGestor() {
        return papel == Papel.GESTOR;
    }

    public boolean isLider() {
        return papel == Papel.LIDER;
    }

    /** Lider ou gerente de RH: entra nas telas de {@code /gestao}. */
    public boolean isAcessaGestao() {
        return papel.acessaGestao();
    }

    /** Nulo para quem nao e lider (ou para o lider ainda sem departamento). */
    public Long getDepartamentoLideradoId() {
        return departamentoLideradoId;
    }

    public String getNomeDoDepartamentoLiderado() {
        return nomeDoDepartamentoLiderado;
    }

    /** Texto do cargo exibido na barra superior. */
    public String getRotuloPapel() {
        return switch (papel) {
            case GESTOR -> "Gerente de RH · todos os departamentos";
            case LIDER -> nomeDoDepartamentoLiderado == null
                    ? "Líder · sem departamento"
                    : "Líder · " + nomeDoDepartamentoLiderado;
            case COLABORADOR -> nomeDoTime;
        };
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(papel.authority()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
