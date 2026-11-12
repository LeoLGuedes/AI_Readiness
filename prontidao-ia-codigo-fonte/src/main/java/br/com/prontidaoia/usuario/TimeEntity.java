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
import java.util.Objects;

/**
 * Time acompanhado pelo sistema.
 *
 * <p>A classe se chama {@code TimeEntity} porque {@code Time} colidiria com
 * {@code java.sql.Time}. A tabela se chama {@code time}, como manda a secao 4,
 * escrita entre crases para que o Hibernate a cite como palavra reservada.</p>
 *
 * <p>Cada time e um <b>departamento</b>. A coluna {@code gestor_id} aponta para
 * o responsavel direto: o lider do departamento (papel LIDER) ou, enquanto o
 * departamento nao tem lider, a gerente de RH.</p>
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

    /** O lider do departamento, ou nulo quando o responsavel ainda e o RH. */
    public Usuario getLider() {
        return gestor != null && gestor.isLider() ? gestor : null;
    }

    /** Coloca um lider (papel LIDER) a frente do departamento. */
    public void definirLider(Usuario lider) {
        Objects.requireNonNull(lider, "lider");
        if (!lider.isLider()) {
            throw new IllegalArgumentException("Somente um usuario com papel LIDER lidera um departamento.");
        }
        this.gestor = lider;
    }
}
