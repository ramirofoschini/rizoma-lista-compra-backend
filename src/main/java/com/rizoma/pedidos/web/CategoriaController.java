package com.rizoma.pedidos.web;

import com.rizoma.pedidos.dto.Dtos.CategoriaItem;
import com.rizoma.pedidos.service.CategoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Listado público de categorías activas (para el desplegable y el catálogo). */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaItem> activas() {
        return categoriaService.activas();
    }
}
