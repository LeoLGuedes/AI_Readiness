package br.com.prontidaoia.atividade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Atividade de letramento com 10 questões (seção 8.1): "1A" … "6B" e a trilha
 * de verificação do Atendimento.
 */
@Entity
@Table(name = "atividade")
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String titulo;

    /**
     * Nível de dificuldade. A troca de escopo nunca muda o nível (seção 5.4),
     * para que o técnico continue comparável entre ciclos.
     */
    @Column(nullable = false)
    private Integer nivel;

    private String descricao;

    protected Atividade() {
        // exigido pelo JPA
    }

    public Atividade(String codigo, String titulo, int nivel, String descricao) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.nivel = nivel;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public Integer getNivel() {
        return nivel;
    }

    public String getDescricao() {
        return descricao;
    }
}
