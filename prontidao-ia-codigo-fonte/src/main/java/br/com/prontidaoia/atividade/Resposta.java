package br.com.prontidaoia.atividade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Resposta a uma questão, gravada quando a tentativa é finalizada. */
@Entity
@Table(name = "resposta")
public class Resposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tentativa_id", nullable = false)
    private Tentativa tentativa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Column(name = "alternativa_escolhida", nullable = false)
    private Integer alternativaEscolhida;

    @Column(nullable = false)
    private Boolean correta;

    /** Intervalo entre esta marcação e a anterior (decisão D17). */
    @Column(name = "tempo_segundos", nullable = false)
    private Integer tempoSegundos;

    protected Resposta() {
        // exigido pelo JPA
    }

    public Resposta(Tentativa tentativa, Questao questao, int alternativaEscolhida,
                    boolean correta, int tempoSegundos) {
        this.tentativa = tentativa;
        this.questao = questao;
        this.alternativaEscolhida = alternativaEscolhida;
        this.correta = correta;
        this.tempoSegundos = tempoSegundos;
    }

    public Long getId() {
        return id;
    }

    public Tentativa getTentativa() {
        return tentativa;
    }

    public Questao getQuestao() {
        return questao;
    }

    public Integer getAlternativaEscolhida() {
        return alternativaEscolhida;
    }

    public Boolean getCorreta() {
        return correta;
    }

    public Integer getTempoSegundos() {
        return tempoSegundos;
    }
}
