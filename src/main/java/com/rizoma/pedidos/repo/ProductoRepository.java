package com.rizoma.pedidos.repo;

import com.rizoma.pedidos.domain.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = "presentaciones")
    List<Producto> findByActivoTrueOrderByCategoriaAscOrdenAsc();

    @EntityGraph(attributePaths = "presentaciones")
    List<Producto> findAllByOrderByCategoriaAscOrdenAsc();
}
