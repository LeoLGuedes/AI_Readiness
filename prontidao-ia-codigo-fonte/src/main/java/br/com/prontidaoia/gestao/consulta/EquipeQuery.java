package br.com.prontidaoia.gestao.consulta;

import br.com.prontidaoia.gestao.AcessoDepartamento;
import br.com.prontidaoia.usuario.Papel;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tela "Equipe": os departamentos que a pessoa da gestão acompanha, quem os
 * lidera e quantos colaboradores ativos cada um tem.
 *
 * <p>Regra de privacidade da seção 4 (invariante 9): nenhuma tela de
 * {@code /gestao} mostra o nome de um colaborador. Por isso a tela traz só
 * contagens, nunca a lista de pessoas.</p>
 */
@Service
public class EquipeQuery {

    private final AcessoDepartamento acesso;
    private final UsuarioRepository usuarios;

    public EquipeQuery(AcessoDepartamento acesso, UsuarioRepository usuarios) {
        this.acesso = acesso;
        this.usuarios = usuarios;
    }

    /**
     * @param lider        nome do líder; nulo quando o departamento responde
     *                     direto ao RH
     * @param colaboradores colaboradores ativos do departamento
     * @param criadosPorMim contas que quem está logado criou neste departamento
     */
    public record ResumoDepartamento(Long id, String nome, String lider, long colaboradores, long criadosPorMim) {
    }

    public record TelaEquipe(List<ResumoDepartamento> departamentos, boolean veTodos, String departamentoLiderado) {

        public boolean podeCriar() {
            return !departamentos.isEmpty();
        }

        /** O líder cria só para o próprio departamento: o campo vem travado. */
        public boolean departamentoFixo() {
            return !veTodos && departamentos.size() == 1;
        }
    }

    @Transactional(readOnly = true)
    public TelaEquipe de(UsuarioAutenticado usuario) {
        List<ResumoDepartamento> resumos = acesso.departamentosVisiveis(usuario).stream()
                .map(t -> resumo(t, usuario.getId()))
                .toList();
        return new TelaEquipe(resumos, acesso.veTodos(usuario), usuario.getNomeDoDepartamentoLiderado());
    }

    private ResumoDepartamento resumo(TimeEntity time, Long quemPede) {
        Usuario lider = time.getLider();
        return new ResumoDepartamento(time.getId(), time.getNome(), lider == null ? null : lider.getNome(),
                usuarios.countByTimeIdAndAtivoTrueAndPapel(time.getId(), Papel.COLABORADOR),
                usuarios.countByCriadoPorIdAndTimeId(quemPede, time.getId()));
    }
}
