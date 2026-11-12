package br.com.prontidaoia.diagnostico.alerta;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.usuario.TimeEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Alerta gerado no fechamento de um ciclo (seção 5.2).
 *
 * <p>Um time tem no máximo um alerta não resolvido de cada tipo (índice
 * único {@code uq_alerta_nao_resolvido}, decisão D11).</p>
 */
@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private TimeEntity time;

    /** Ciclo em cujo fechamento o alerta nasceu. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeAlerta prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAlerta status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciclo_resolucao_id")
    private Ciclo cicloResolucao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private DetalheAlerta detalhe;

    protected Alerta() {
        // exigido pelo JPA
    }

    public Alerta(TimeEntity time, Ciclo ciclo, TipoAlerta tipo, PrioridadeAlerta prioridade, DetalheAlerta detalhe) {
        this.time = time;
        this.ciclo = ciclo;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.status = StatusAlerta.ABERTO;
        this.detalhe = detalhe;
    }

    /** "Sem duplicatas" da seção 5.2: a regra voltou a valer, o detalhe é atualizado. */
    public void atualizarDetalhe(DetalheAlerta novo) {
        this.detalhe = novo;
    }

    /** A gestão salvou o escopo (seção 5.4). */
    public void passarAAcompanhamento() {
        this.status = StatusAlerta.EM_ACOMPANHAMENTO;
    }

    public void resolver(Ciclo noFechamentoDe) {
        this.status = StatusAlerta.RESOLVIDO;
        this.cicloResolucao = noFechamentoDe;
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

    public TipoAlerta getTipo() {
        return tipo;
    }

    public PrioridadeAlerta getPrioridade() {
        return prioridade;
    }

    public StatusAlerta getStatus() {
        return status;
    }

    public Ciclo getCicloResolucao() {
        return cicloResolucao;
    }

    public DetalheAlerta getDetalhe() {
        return detalhe;
    }
}
