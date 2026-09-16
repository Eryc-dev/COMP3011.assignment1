package comp3011.assignment1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the COMP3011 Assignment 1 speech-to-text web site.
 *
 * <p>Package layout:
 * <ul>
 *   <li>{@code config} - Spring configuration (outbound OpenAI HTTP client).</li>
 *   <li>{@code controller} - REST controllers (transcription, administration, statistics).</li>
 *   <li>{@code dto} - JSON request/response records matching the YAML specification.</li>
 *   <li>{@code exception} - custom exceptions and the global JSON error handler.</li>
 *   <li>{@code service} - business logic (OpenAI STT, token counters, server lifecycle).</li>
 * </ul>
 */
@SpringBootApplication
public class Assignment1Application {

    public static void main(String[] args) {
        SpringApplication.run(Assignment1Application.class, args);
    }
}
