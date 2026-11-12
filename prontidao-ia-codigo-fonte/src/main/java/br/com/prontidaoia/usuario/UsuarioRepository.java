package br.com.prontidaoia.usuario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Usuario> findByIdAndAtivoTrue(Long id);

    List<Usuario> findByTimeIdOrderByNome(Long timeId);

    List<Usuario> findByTimeIdAndAtivoTrueAndAutocadastroFalseOrderByNome(Long timeId);

    long countByTimeId(Long timeId);

    long countByTimeIdAndAtivoTrue(Long timeId);

    List<Usuario> findByPapelOrderByNome(Papel papel);
}
