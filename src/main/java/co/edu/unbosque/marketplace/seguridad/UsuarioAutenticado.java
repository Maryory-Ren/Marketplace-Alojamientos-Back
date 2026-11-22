package co.edu.unbosque.marketplace.seguridad;

import java.util.UUID;

public record UsuarioAutenticado(UUID id, String rol) {}
