package br.com.prontidaoia.diagnostico.recomendacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Intervenção do catálogo da seção 5.3. Vem da migration V2 e não é editada. */
@Entity
@Table(name = "intervencao")
public class Intervencao {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private String executor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoIntervencao tipo;

    protected Intervencao() {
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

    public String getExecutor() {
        return executor;
    }

    public TipoIntervencao getTipo() {
        return tipo;
    }
}
