package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.config.DemoProperties;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria a gestora, os três times e os 24 colaboradores do cenário (seção 3).
 *
 * <p>Só o elenco: check-ins, tentativas e respostas são gerados pelo painel de
 * demonstração. A seção 5.5 proíbe inserir indicadores ou alertas direto no
 * banco, e esta classe não toca em nenhum dos dois.</p>
 */
@Component
public class SeedPessoas {

    private final UsuarioRepository usuarios;
    private final TimeRepository times;
    private final PasswordEncoder encoder;
    private final DemoProperties demo;

    public SeedPessoas(UsuarioRepository usuarios,
                       TimeRepository times,
                       PasswordEncoder encoder,
                       DemoProperties demo) {
        this.usuarios = usuarios;
        this.times = times;
        this.encoder = encoder;
        this.demo = demo;
    }

    /** Verdadeiro quando o elenco já existe; usado para não semear duas vezes. */
    public boolean jaSemeado() {
        return usuarios.findByEmail(PessoasDemo.email(PessoasDemo.GESTORA)).isPresent();
    }

    /**
     * Semeia o elenco, se ainda não existir.
     *
     * <p>A ordem importa: a gestora entra primeiro porque o time aponta para
     * ela, e os colaboradores entram por último porque apontam para o time.</p>
     */
    @Transactional
    public void semear() {
        if (jaSemeado()) {
            return;
        }

        String senha = encoder.encode(demo.senhaPadrao());

        Usuario renata = usuarios.save(Usuario.gestor(
                PessoasDemo.GESTORA,
                PessoasDemo.email(PessoasDemo.GESTORA),
                senha));

        for (PessoasDemo.TimeDemo timeDemo : PessoasDemo.times()) {
            TimeEntity time = times.save(new TimeEntity(timeDemo.nome(), renata));
            for (String nome : timeDemo.membros()) {
                usuarios.save(Usuario.colaborador(nome, PessoasDemo.email(nome), senha, time));
            }
        }
    }
}
