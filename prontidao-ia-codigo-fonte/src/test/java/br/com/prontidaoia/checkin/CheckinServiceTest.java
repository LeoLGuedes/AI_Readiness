package br.com.prontidaoia.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras do check-in. Cada teste roda numa transação desfeita no fim, então
 * todos partem do ciclo 1 aberto e sem nenhum check-in.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
@DisplayName("Check-in")
class CheckinServiceTest extends TesteComBanco {

    @Autowired
    private CheckinService service;

    @Autowired
    private CheckinRepository checkins;

    @Autowired
    private DificuldadeRepository dificuldades;

    @Autowired
    private UsuarioRepository usuarios;

    private Long carla;
    private Long receioSubstituicao;
    private Long naoSeiQuandoConfiar;

    @BeforeEach
    void preparar() {
        carla = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.CARLA)).orElseThrow().getId();
        List<Dificuldade> catalogo = dificuldades.findAllByOrderById();
        receioSubstituicao = catalogo.get(0).getId();
        naoSeiQuandoConfiar = catalogo.get(1).getId();
    }

    @Test
    @DisplayName("grava confiança, receio e as dificuldades marcadas")
    void gravaOCheckin() {
        Checkin salvo = service.registrar(carla,
                new RespostaCheckin(2, 4, List.of(receioSubstituicao, naoSeiQuandoConfiar), false));

        Checkin lido = checkins.findById(salvo.getId()).orElseThrow();
        assertThat(lido.getConfianca()).isEqualTo(2);
        assertThat(lido.getReceioSubstituicao()).isEqualTo(4);
        assertThat(lido.getDificuldades())
                .extracting(Dificuldade::getCodigo)
                .containsExactlyInAnyOrder("RECEIO_SUBSTITUICAO", "NAO_SEI_QUANDO_CONFIAR");
    }

    @Test
    @DisplayName("o segundo check-in no mesmo ciclo é recusado")
    void checkinDuplicadoERecusado() {
        service.registrar(carla, new RespostaCheckin(2, 4, List.of(receioSubstituicao), false));

        assertThatThrownBy(() ->
                service.registrar(carla, new RespostaCheckin(5, 1, List.of(), true)))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessage("Você já fez o check-in deste ciclo.");

        assertThat(checkins.findByUsuarioId(carla)).hasSize(1)
                .first().extracting(Checkin::getConfianca).isEqualTo(2);
    }

    @Test
    @DisplayName("“nenhuma” grava o check-in sem dificuldades")
    void nenhumaGravaSemDificuldades() {
        Checkin salvo = service.registrar(carla, new RespostaCheckin(4, 2, List.of(), true));

        assertThat(checkins.findById(salvo.getId()).orElseThrow().getDificuldades()).isEmpty();
    }

    @Test
    @DisplayName("“nenhuma” junto com outra dificuldade é recusado")
    void nenhumaNaoSeCombina() {
        assertThatThrownBy(() ->
                service.registrar(carla, new RespostaCheckin(4, 2, List.of(receioSubstituicao), true)))
                .isInstanceOf(RegraVioladaException.class);
        assertThat(checkins.findByUsuarioId(carla)).isEmpty();
    }

    @Test
    @DisplayName("a pergunta 3 sem resposta é recusada")
    void pergunta3Obrigatoria() {
        assertThatThrownBy(() ->
                service.registrar(carla, new RespostaCheckin(4, 2, List.of(), false)))
                .isInstanceOf(RegraVioladaException.class);
    }

    @Test
    @DisplayName("valores fora da escala de 1 a 5 são recusados")
    void foraDaEscala() {
        assertThatThrownBy(() ->
                service.registrar(carla, new RespostaCheckin(0, 2, List.of(), true)))
                .isInstanceOf(RegraVioladaException.class);
        assertThatThrownBy(() ->
                service.registrar(carla, new RespostaCheckin(3, 6, List.of(), true)))
                .isInstanceOf(RegraVioladaException.class);
    }
}
