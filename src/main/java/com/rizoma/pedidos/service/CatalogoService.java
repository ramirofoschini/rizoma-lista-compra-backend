package com.rizoma.pedidos.service;

import com.rizoma.pedidos.domain.Categoria;
import com.rizoma.pedidos.domain.Presentacion;
import com.rizoma.pedidos.domain.Producto;
import com.rizoma.pedidos.dto.Dtos.*;
import com.rizoma.pedidos.repo.CategoriaRepository;
import com.rizoma.pedidos.repo.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogoService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    public CatalogoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    /** Catálogo público agrupado por categoría (solo activos), en orden de categoría. */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> catalogoPorCategoria() {
        Map<String, List<ProductoDTO>> porCat = new LinkedHashMap<>();
        for (Producto p : productoRepo.findActivosOrdenados()) {
            List<PresentacionDTO> pres = p.getPresentaciones().stream()
                    .filter(Presentacion::isActivo)
                    .map(this::toPresentacionDTO)
                    .toList();
            if (pres.isEmpty()) continue;
            porCat.computeIfAbsent(p.getCategoria().getNombre(), k -> new ArrayList<>())
                    .add(toProductoDTO(p, pres));
        }
        return porCat.entrySet().stream()
                .map(e -> new CategoriaDTO(e.getKey(), e.getValue()))
                .toList();
    }

    /** Listado plano para el panel admin (incluye inactivos). */
    @Transactional(readOnly = true)
    public List<ProductoDTO> todos() {
        return productoRepo.findTodosOrdenados().stream()
                .map(p -> toProductoDTO(p, p.getPresentaciones().stream().map(this::toPresentacionDTO).toList()))
                .toList();
    }

    @Transactional
    public ProductoDTO crear(ProductoInput in) {
        Producto p = new Producto();
        aplicar(p, in);
        productoRepo.save(p);
        return toProductoDTO(p, p.getPresentaciones().stream().map(this::toPresentacionDTO).toList());
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoInput in) {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        aplicar(p, in);
        productoRepo.save(p);
        return toProductoDTO(p, p.getPresentaciones().stream().map(this::toPresentacionDTO).toList());
    }

    @Transactional
    public void desactivar(Long id) {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        p.setActivo(false);
        productoRepo.save(p);
    }

    private void aplicar(Producto p, ProductoInput in) {
        Categoria cat = categoriaRepo.findById(in.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría inexistente"));
        p.setCategoria(cat);
        p.setMarca(blankToNull(in.marca()));
        p.setNombre(in.nombre());
        p.setNotas(blankToNull(in.notas()));
        p.setActivo(in.activo() == null || in.activo());
        p.getPresentaciones().clear();
        if (in.presentaciones() != null) {
            for (PresentacionInput pi : in.presentaciones()) {
                Presentacion pr = new Presentacion();
                pr.setEtiqueta(blankToNull(pi.etiqueta()));
                pr.setPrecio(pi.precio());
                pr.setActivo(pi.activo() == null || pi.activo());
                p.addPresentacion(pr);
            }
        }
    }

    private PresentacionDTO toPresentacionDTO(Presentacion pr) {
        return new PresentacionDTO(pr.getId(), pr.getEtiqueta(), pr.getPrecio(), pr.getPrecio() != null);
    }

    private ProductoDTO toProductoDTO(Producto p, List<PresentacionDTO> pres) {
        return new ProductoDTO(p.getId(), p.getCategoria().getId(), p.getCategoria().getNombre(),
                p.getMarca(), p.getNombre(), p.getNotas(), pres);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
