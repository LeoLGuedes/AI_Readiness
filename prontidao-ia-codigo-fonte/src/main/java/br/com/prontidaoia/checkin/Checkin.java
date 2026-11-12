package br.com.prontidaoia.checkin;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Check-in semanal de uma pessoa: a fonte de percepção do artefato (seção 1).
 *
 * <p>Nenhuma consulta do gestor lê esta tabela linha a linha; ela só chega à
 * gestão agregada por time e com amostra mínima (regra de privacidade da
 * seção 4).</p>
 */
@Entity
@Table(name = "checkin")
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    /** Pergunta 1, de 1 (nada seguro) a 5 (totalmente seguro). */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private Integer confianca;

    /** Pergunta 2, de 1 (discordo totalmente) a 5 (concordo totalmente). */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "receio_substituicao", nullable = false)
    private Integer receioSubstituicao;

    @Column(name = "respondido_em", nullable = false)
    private Instant respondidoEm;

    /** Pergunta 3. Vazio quando a pessoa marcou "nenhuma" (decisão D18). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "checkin_dificuldade",
            joinColumns = @JoinColumn(name = "checkin_id"),
            inverseJoinColumns = @JoinColumn(name = "dificuldade_id"))
    private Set<Dificuldade> dificuldades = new LinkedHashSet<>();

    protected Checkin() {
        // exigido pelo JPA
    }

    public Checkin(Usuario usuario, Ciclo ciclo, int confianca, int receioSubstituicao,
                   Set<Dificuldade> dificuldades, Instant respondidoEm) {
        this.usuario = usuario;
        this.ciclo = ciclo;
        this.confianca = confianca;
        this.receioSubstituicao = receioSubstituicao;
        this.dificuldades = new LinkedHashSet<>(dificuldades);
        this.respondidoEm = respondidoEm;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public Integer getConfianca() {
        return confianca;
    }

    public Integer getReceioSubstituicao() {
        return receioSubstituicao;
    }

    public Instant getRespondidoEm() {
        return respondidoEm;
    }

    public Set<Dificuldade> getDificuldades() {
        return dificuldades;
    }
}
