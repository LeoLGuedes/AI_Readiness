package br.com.prontidaoia.checkin;

import br.com.prontidaoia.diagnostico.recomendacao.Intervencao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Dificuldade do catálogo da seção 5.3, marcada na pergunta 3 do check-in.
 *
 * <p>Cada uma corresponde a um fator de adoção (ver {@code docs/fundamentacao.md})
 * e aponta para a intervenção que ataca esse fator.</p>
 */
@Entity
@Table(name = "dificuldade")
public class Dificuldade {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private String fator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervencao_id", nullable = false)
    private Intervencao intervencao;

    protected Dificuldade() {
        // exigido pelo JPA
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getFator() {
        return fator;
    }

    public Intervencao getIntervencao() {
        return intervencao;
    }
}
