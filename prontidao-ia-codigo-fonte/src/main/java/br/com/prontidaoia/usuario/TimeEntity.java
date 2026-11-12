package br.com.prontidaoia.usuario;

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
 * Time acompanhado pelo sistema.
 *
 * <p>A classe se chama {@code TimeEntity} porque {@code Time} colidiria com
 * {@code java.sql.Time}. A tabela se chama {@code time}, como manda a secao 4,
 * escrita entre crases para que o Hibernate a cite como palavra reservada.</p>
 */
@Entity
@Table(name = "`time`")
public class TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Usuario gestor;

    protected TimeEntity() {
        // exigido pelo JPA
    }

    public TimeEntity(String nome, Usuario gestor) {
        this.nome = nome;
        this.gestor = gestor;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Usuario getGestor() {
        return gestor;
    }
}
