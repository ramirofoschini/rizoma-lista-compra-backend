package com.rizoma.pedidos.repo;

import com.rizoma.pedidos.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivaTrueOrderByOrdenAscNombreAsc();

    List<Categoria> findAllByOrderByOrdenAscNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}
