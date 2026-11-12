package br.com.prontidaoia.diagnostico.indicador;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.diagnostico.indicador.CalculadoraIndicadores.Indicadores;
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
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fotografia dos indicadores de um time num ciclo, gravada no fechamento.
 *
 * <p><b>Nunca é recalculada</b> (seção 4): a classe não tem setters, e o
 * índice único (time, ciclo) impede uma segunda gravação.</p>
 */
@Entity
@Table(name = "indicador_time_ciclo")
public class IndicadorTimeCiclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private TimeEntity time;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @Column(nullable = false)
    private BigDecimal tecnico;

    /** Nula quando o time não teve a amostra mínima de check-ins. */
    private BigDecimal confianca;

    /** Nula quando o time não teve a amostra mínima de check-ins. */
    private BigDecimal resistencia;

    @Column(nullable = false)
    private BigDecimal engajamento;

    @Column(name = "tempo_medio_min", nullable = false)
    private BigDecimal tempoMedioMin;

    @Column(name = "n_checkins", nullable = false)
    private Integer nCheckins;

    @Column(name = "n_membros", nullable = false)
    private Integer nMembros;

    @Column(name = "calculado_em", nullable = false)
    private Instant calculadoEm;

    protected IndicadorTimeCiclo() {
        // exigido pelo JPA
    }

    public IndicadorTimeCiclo(TimeEntity time, Ciclo ciclo, Indicadores valores, Instant calculadoEm) {
        this.time = time;
        this.ciclo = ciclo;
        this.tecnico = valores.tecnico();
        this.confianca = valores.confianca();
        this.resistencia = valores.resistencia();
        this.engajamento = valores.engajamento();
        this.tempoMedioMin = valores.tempoMedioMin();
        this.nCheckins = valores.checkins();
        this.nMembros = valores.membros();
        this.calculadoEm = calculadoEm;
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

    public BigDecimal getTecnico() {
        return tecnico;
    }

    public BigDecimal getConfianca() {
        return confianca;
    }

    public BigDecimal getResistencia() {
        return resistencia;
    }

    public BigDecimal getEngajamento() {
        return engajamento;
    }

    public BigDecimal getTempoMedioMin() {
        return tempoMedioMin;
    }

    public Integer getNCheckins() {
        return nCheckins;
    }

    public Integer getNMembros() {
        return nMembros;
    }

    public Instant getCalculadoEm() {
        return calculadoEm;
    }
}
