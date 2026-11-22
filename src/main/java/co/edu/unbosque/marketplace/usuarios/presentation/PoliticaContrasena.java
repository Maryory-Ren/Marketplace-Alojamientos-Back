package co.edu.unbosque.marketplace.usuarios.presentation;

final class PoliticaContrasena {

    static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,128}$";
    static final String MENSAJE =
            "La contraseña debe tener entre 8 y 128 caracteres e incluir mayúscula, minúscula, número y símbolo";

    private PoliticaContrasena() {}
}
