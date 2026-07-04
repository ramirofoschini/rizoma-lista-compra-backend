package com.rizoma.pedidos.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Configuración pública que el front necesita (número de WhatsApp, días de entrega). */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${app.whatsapp.destino}")
    private String whatsappDestino;

    @GetMapping
    public Map<String, Object> config() {
        return Map.of(
                "whatsappDestino", whatsappDestino,
                "diasEntrega", List.of("MARTES", "JUEVES"),
                "horarioEntrega", "18 a 21 hs"
        );
    }
}
