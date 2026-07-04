package com.rizoma.pedidos.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "marca")
    private String marca;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "notas", columnDefinition = "text")
    private String notas;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** Orden original en el listado, para mantener el mismo orden que el folleto. */
    @Column(name = "orden")
    private Integer orden;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Presentacion> presentaciones = new ArrayList<>();

    public void addPresentacion(Presentacion p) {
        p.setProducto(this);
        this.presentaciones.add(p);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public List<Presentacion> getPresentaciones() { return presentaciones; }
    public void setPresentaciones(List<Presentacion> presentaciones) { this.presentaciones = presentaciones; }
}
