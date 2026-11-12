package br.com.prontidaoia.atividade;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.usuario.TimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma atividade liberada a um time num ciclo. O conjunto delas é o
 * <b>escopo</b> do time no ciclo (seção 4).
 */
@Entity
@Table(name = "atividade_liberada")
public class AtividadeLiberada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private TimeEntity time;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    /** Preenchido quando a liberação veio de uma troca de escopo (seção 5.4). */
    @Column(name = "recomendacao_id")
    private Long recomendacaoId;

    protected AtividadeLiberada() {
        // exigido pelo JPA
    }

    public AtividadeLiberada(TimeEntity time, Ciclo ciclo, Atividade atividade) {
        this.time = time;
        this.ciclo = ciclo;
        this.atividade = atividade;
    }

    /**
     * Liberação que carrega a recomendação de origem. Usada ao copiar o
     * escopo para o ciclo seguinte e na troca de escopo da seção 5.4.
     */
    public AtividadeLiberada(TimeEntity time, Ciclo ciclo, Atividade atividade, Long recomendacaoId) {
        this(time, ciclo, atividade);
        this.recomendacaoId = recomendacaoId;
    }

    public Long getId() {
        return id;
    }

    public TimeEntity getTime() {
        return time;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public Atividade getAtividade() {
        return atividade;
    }

    public Long getRecomendacaoId() {
        return recomendacaoId;
    }
}
