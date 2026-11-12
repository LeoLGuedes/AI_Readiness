package br.com.prontidaoia.checkin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DificuldadeRepository extends JpaRepository<Dificuldade, Long> {

    /** Na ordem do catálogo, que é a ordem de id (decisão D10). */
    List<Dificuldade> findAllByOrderById();
}
