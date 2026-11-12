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
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Questão de múltipla escolha, com o gabarito comentado. */
@Entity
@Table(name = "questao")
public class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false)
    private String enunciado;

    /** Textos das alternativas, na ordem exibida. Guardado como jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> alternativas;

    /** Posição da alternativa correta em {@link #alternativas}, a partir de 0. */
    @Column(name = "indice_correto", nullable = false)
    private Integer indiceCorreto;

    @Column(nullable = false)
    private String comentario;

    protected Questao() {
        // exigido pelo JPA
    }

    public Questao(Atividade atividade, Area area, int ordem, String enunciado,
                   List<String> alternativas, int indiceCorreto, String comentario) {
        this.atividade = atividade;
        this.area = area;
        this.ordem = ordem;
        this.enunciado = enunciado;
        this.alternativas = List.copyOf(alternativas);
        this.indiceCorreto = indiceCorreto;
        this.comentario = comentario;
    }

    public Long getId() {
        return id;
    }

    public Atividade getAtividade() {
        return atividade;
    }

    public Area getArea() {
        return area;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public List<String> getAlternativas() {
        return alternativas;
    }

    public Integer getIndiceCorreto() {
        return indiceCorreto;
    }

    public String getComentario() {
        return comentario;
    }
}
