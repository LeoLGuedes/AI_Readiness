package br.com.prontidaoia.ciclo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Um ciclo semanal de acompanhamento. */
@Entity
@Table(name = "ciclo")
public class Ciclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCiclo status;

    protected Ciclo() {
        // exigido pelo JPA
    }

    public Ciclo(Integer numero, LocalDate inicio, LocalDate fim) {
        this.numero = numero;
        this.inicio = inicio;
        this.fim = fim;
        this.status = StatusCiclo.ABERTO;
    }

    /**
     * Marca o ciclo como fechado. Etapa 1 do fechamento (secao 5.5): o que nao
     * foi concluido ate aqui fica como nao realizado.
     */
    public void fechar() {
        this.status = StatusCiclo.FECHADO;
    }

    public boolean estaAberto() {
        return status == StatusCiclo.ABERTO;
    }

    /** Verdadeiro quando a data de fim ja passou e o agendador deve fechar. */
    public boolean venceuEm(LocalDate hoje) {
        return estaAberto() && hoje.isAfter(fim);
    }

    public Long getId() {
        return id;
    }

    public Integer getNumero() {
        return numero;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public StatusCiclo getStatus() {
        return status;
    }
}
