package br.com.prontidaoia.diagnostico.recomendacao;

import br.com.prontidaoia.checkin.Dificuldade;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Um item recomendado para um alerta (seção 5.3): uma intervenção, a
 * dificuldade que a motivou e quantas respostas a marcaram.
 */
@Entity
@Table(name = "recomendacao")
public class Recomendacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervencao_id", nullable = false)
    private Intervencao intervencao;

    /** Nula na recomendação do alerta B, que não vem de dificuldade. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dificuldade_id")
    private Dificuldade dificuldade;

    /** Quantos check-ins marcaram a dificuldade ("5 de 7 respostas"). */
    @Column(name = "n_respostas", nullable = false)
    private Integer nRespostas;

    @Column(name = "total_respostas", nullable = false)
    private Integer totalRespostas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRecomendacao status;

    @Column(name = "descricao_ajustada")
    private String descricaoAjustada;

    @Column(name = "data_prevista")
    private LocalDate dataPrevista;

    /** Primeiro ciclo em que a intervenção valeu; base do encerramento do alerta A. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciclo_aplicacao_id")
    private Ciclo cicloAplicacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decidido_por")
    private Usuario decididoPor;

    @Column(name = "decidido_em")
    private Instant decididoEm;

    protected Recomendacao() {
        // exigido pelo JPA
    }

    public Recomendacao(Alerta alerta, Intervencao intervencao, Dificuldade dificuldade,
                        int nRespostas, int totalRespostas) {
        this.alerta = alerta;
        this.intervencao = intervencao;
        this.dificuldade = dificuldade;
        this.nRespostas = nRespostas;
        this.totalRespostas = totalRespostas;
        this.status = StatusRecomendacao.SUGERIDA;
    }

    /** Decisão da gestão: aceitar ou adiar. */
    public void decidir(StatusRecomendacao novo, Usuario quem, Instant quando) {
        this.status = novo;
        this.decididoPor = quem;
        this.decididoEm = quando;
    }

    /** Fluxo alternativo FA1: o texto passa a ser o da gestão. */
    public void ajustar(String texto, Usuario quem, Instant quando) {
        this.descricaoAjustada = texto;
        decidir(StatusRecomendacao.AJUSTADA, quem, quando);
    }

    /** Intervenção humana marcada como agendada. */
    public void agendar(LocalDate data) {
        this.dataPrevista = data;
    }

    /** Aceita ou ajustada: vale para a troca de escopo. */
    public boolean foiAceita() {
        return status == StatusRecomendacao.ACEITA || status == StatusRecomendacao.AJUSTADA;
    }

    /** O texto que vale: o ajustado pela gestão, se houver, ou o do catálogo. */
    public String textoVigente() {
        return descricaoAjustada != null ? descricaoAjustada : intervencao.getDescricao();
    }

    /** Registra o ciclo em que a troca de escopo passou a valer (seção 5.4). */
    public void registrarAplicacao(Ciclo ciclo) {
        this.cicloAplicacao = ciclo;
    }

    public Long getId() {
        return id;
    }

    public Alerta getAlerta() {
        return alerta;
    }

    public Intervencao getIntervencao() {
        return intervencao;
    }

    public Dificuldade getDificuldade() {
        return dificuldade;
    }

    public Integer getNRespostas() {
        return nRespostas;
    }

    public Integer getTotalRespostas() {
        return totalRespostas;
    }

    public StatusRecomendacao getStatus() {
        return status;
    }

    public String getDescricaoAjustada() {
        return descricaoAjustada;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public Ciclo getCicloAplicacao() {
        return cicloAplicacao;
    }

    public Usuario getDecididoPor() {
        return decididoPor;
    }

    public Instant getDecididoEm() {
        return decididoEm;
    }
}
