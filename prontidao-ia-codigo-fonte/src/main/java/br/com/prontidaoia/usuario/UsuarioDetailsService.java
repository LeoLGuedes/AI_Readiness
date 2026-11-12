package br.com.prontidaoia.usuario;

import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o usuario pelo e-mail no login por formulario. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;
    private final TimeRepository times;

    public UsuarioDetailsService(UsuarioRepository usuarios, TimeRepository times) {
        this.usuarios = usuarios;
        this.times = times;
    }

    /**
     * Transacional para que o time (mapeado como LAZY) possa ser lido enquanto
     * a sessao do JPA ainda esta aberta; {@link UsuarioAutenticado} guarda os
     * valores ja resolvidos.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarios.findByEmail(email == null ? "" : email.trim().toLowerCase(Locale.ROOT))
                .map(u -> new UsuarioAutenticado(u, departamentoLiderado(u)))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
    }

    /** O departamento do lider vem do banco no login, nunca de parametro da URL. */
    private TimeEntity departamentoLiderado(Usuario usuario) {
        if (!usuario.isLider()) {
            return null;
        }
        return times.findByGestorIdOrderByNome(usuario.getId()).stream().findFirst().orElse(null);
    }
}
