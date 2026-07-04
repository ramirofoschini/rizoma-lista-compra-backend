package com.rizoma.pedidos.service;

import com.rizoma.pedidos.domain.Categoria;
import com.rizoma.pedidos.dto.Dtos.CategoriaInput;
import com.rizoma.pedidos.dto.Dtos.CategoriaItem;
import com.rizoma.pedidos.repo.CategoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class CategoriaService {

    private final CategoriaRepository repo;

    public CategoriaService(CategoriaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CategoriaItem> activas() {
        return repo.findByActivaTrueOrderByOrdenAscNombreAsc().stream().map(this::toItem).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaItem> todas() {
        return repo.findAllByOrderByOrdenAscNombreAsc().stream().map(this::toItem).toList();
    }

    @Transactional
    public CategoriaItem crear(CategoriaInput in) {
        String nombre = in.nombre().trim();
        repo.findByNombreIgnoreCase(nombre).ifPresent(c -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre.");
        });
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setOrden(in.orden() != null ? in.orden() : siguienteOrden());
        c.setActiva(in.activa() == null || in.activa());
        repo.save(c);
        return toItem(c);
    }

    @Transactional
    public CategoriaItem actualizar(Long id, CategoriaInput in) {
        Categoria c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        String nombre = in.nombre().trim();
        repo.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra categoría con ese nombre.");
                });
        c.setNombre(nombre);
        if (in.orden() != null) c.setOrden(in.orden());
        if (in.activa() != null) c.setActiva(in.activa());
        repo.save(c);
        return toItem(c);
    }

    @Transactional
    public void desactivar(Long id) {
        Categoria c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        c.setActiva(false);
        repo.save(c);
    }

    private int siguienteOrden() {
        return repo.findAll().stream()
                .map(Categoria::getOrden).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private CategoriaItem toItem(Categoria c) {
        return new CategoriaItem(c.getId(), c.getNombre(), c.getOrden(), c.isActiva());
    }
}
