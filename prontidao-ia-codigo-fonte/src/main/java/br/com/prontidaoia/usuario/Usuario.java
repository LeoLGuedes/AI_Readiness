package br.com.prontidaoia.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/** Pessoa que usa o sistema: colaborador de um time ou gestor. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean autocadastro = false;

    /** Nulo para o gestor; obrigatorio para o colaborador (secao 4). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_id")
    private TimeEntity time;

    protected Usuario() {
        // exigido pelo JPA
    }

    private Usuario(String nome, String email, String senhaHash, Papel papel, TimeEntity time) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.time = time;
    }

    /**
     * Cria um colaborador. O time e obrigatorio: e a restricao da secao 4,
     * repetida aqui para que a violacao apareca como erro de dominio antes de
     * virar erro de banco.
     */
    public static Usuario colaborador(String nome, String email, String senhaHash, TimeEntity time) {
        Objects.requireNonNull(time, "colaborador precisa de um time");
        return new Usuario(nome, email, senhaHash, Papel.COLABORADOR, time);
    }

    /** Autocadastro: nunca aceita papel vindo do navegador. */
    public static Usuario novoColaborador(String nome, String email, String senhaHash, TimeEntity time) {
        Usuario usuario = colaborador(nome, email, senhaHash, time);
        usuario.autocadastro = true;
        return usuario;
    }

    /** Edicao permitida somente para contas de autocadastro. */
    public void atualizarCadastro(String nome, String email, TimeEntity time, String novoHash) {
        if (!autocadastro || !ativo || papel != Papel.COLABORADOR) {
            throw new IllegalStateException("Conta do cenário protegida contra alterações.");
        }
        this.nome = nome;
        this.email = email;
        this.time = Objects.requireNonNull(time);
        if (novoHash != null) {
            this.senhaHash = novoHash;
        }
    }

    /** Exclusao logica com desidentificacao para manter FKs e registros historicos. */
    public void excluirCadastro(String hashAleatorio) {
        if (!autocadastro || !ativo || papel != Papel.COLABORADOR) {
            throw new IllegalStateException("Esta conta não pode ser excluída.");
        }
        this.ativo = false;
        this.nome = "Colaborador excluído";
        this.email = "conta-excluida-" + id + "@example.invalid";
        this.senhaHash = hashAleatorio;
    }

    /** Cria um gestor, que nao pertence a um time. */
    public static Usuario gestor(String nome, String email, String senhaHash) {
        return new Usuario(nome, email, senhaHash, Papel.GESTOR, null);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    /** Primeiro nome, usado nos cumprimentos das telas do colaborador. */
    public String getPrimeiroNome() {
        int espaco = nome.indexOf(' ');
        return espaco < 0 ? nome : nome.substring(0, espaco);
    }

    /** Iniciais, usadas no avatar da barra superior. */
    public String getIniciais() {
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public TimeEntity getTime() {
        return time;
    }

    public boolean isGestor() {
        return papel == Papel.GESTOR;
    }

    public boolean isAtivo() { return ativo; }

    public boolean isAutocadastro() { return autocadastro; }
}
