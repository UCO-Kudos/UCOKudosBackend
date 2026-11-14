package co.edu.uco.kudos.infraestructura.output.adapter;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor HTTP local (incluido en el JDK) que hace de Brevo o Twilio en las pruebas:
 * guarda cada petición y responde los códigos indicados, en orden (el último se repite).
 * Así se prueba el adaptador real, con su RestClient, sin red ni Docker.
 */
public class ServidorHttpFalso implements AutoCloseable {

    public record Peticion(String metodo, String ruta, String contentType, String autorizacion, String apiKey,
                           String cuerpo) {
    }

    private final HttpServer servidor;
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();

    public ServidorHttpFalso(int... codigos) {
        try {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", intercambio -> {
            var cabeceras = intercambio.getRequestHeaders();
            peticiones.add(new Peticion(intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(),
                    cabeceras.getFirst("Content-Type"), cabeceras.getFirst("Authorization"),
                    cabeceras.getFirst("api-key"),
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            int codigo = codigos[Math.min(peticiones.size(), codigos.length) - 1];
            byte[] respuesta = "{}".getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(codigo, respuesta.length);
            intercambio.getResponseBody().write(respuesta);
            intercambio.close();
        });
        servidor.start();
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public List<Peticion> peticiones() {
        return peticiones;
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
