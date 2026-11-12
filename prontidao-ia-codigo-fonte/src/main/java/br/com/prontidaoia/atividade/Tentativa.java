package br.com.prontidaoia.atividade;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * Uma pessoa fazendo uma atividade liberada. Nasce quando a pessoa abre a
 * atividade e termina quando ela finaliza.
 *
 * <p>Só tentativas finalizadas contam no técnico e na produtividade
 * (decisão D16).</p>
 */
@Entity
@Table(name = "tentativa")
public class Tentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atividade_liberada_id", nullable = false)
    private AtividadeLiberada atividadeLiberada;

    @Column(name = "iniciada_em", nullable = false)
    private Instant iniciadaEm;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    protected Tentativa() {
        // exigido pelo JPA
    }

    public Tentativa(Usuario usuario, AtividadeLiberada atividadeLiberada, Instant iniciadaEm) {
        this.usuario = usuario;
        this.atividadeLiberada = atividadeLiberada;
        this.iniciadaEm = iniciadaEm;
    }

    /**
     * Grava o fim da tentativa. Só acontece uma vez: uma tentativa finalizada
     * não é reaberta nem refeita.
     */
    public void finalizar(Instant quando) {
        if (estaFinalizada()) {
            throw new RegraVioladaException("Esta atividade já foi finalizada.");
        }
        this.finalizadaEm = quando;
    }

    public boolean estaFinalizada() {
        return finalizadaEm != null;
    }

    /** Duração usada na produtividade da seção 5.1: finalizada_em − iniciada_em. */
    public Duration duracao() {
        return Duration.between(iniciadaEm, finalizadaEm);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public AtividadeLiberada getAtividadeLiberada() {
        return atividadeLiberada;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }
}
