package br.com.prontidaoia.atividade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma das quatro áreas de letramento em IA da seção 4: FORMULACAO,
 * VERIFICACAO, DADOS e APLICACAO. Vem da migration V2.
 */
@Entity
@Table(name = "area")
public class Area {

    public static final String FORMULACAO = "FORMULACAO";
    public static final String VERIFICACAO = "VERIFICACAO";
    public static final String DADOS = "DADOS";
    public static final String APLICACAO = "APLICACAO";

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    protected Area() {
        // exigido pelo JPA
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }
}
