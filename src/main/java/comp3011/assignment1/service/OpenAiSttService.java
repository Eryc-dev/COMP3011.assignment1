package comp3011.assignment1.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.exception.InvalidAudioException;
import comp3011.assignment1.exception.SpeechToTextException;

/**
 * Sends recorded audio to the OpenAI {@code /audio/transcriptions} endpoint using the
 * {@code gpt-4o-mini-transcribe} model and records the reported token usage.
 *
 * <p>The call is a normal blocking HTTP request. Because the application runs request handling on
 * Java virtual threads ({@code spring.threads.virtual.enabled=true}), a blocked call parks a cheap
 * virtual thread instead of an OS thread, so hundreds of overlapping transcriptions and API
 * queries do not starve each other.
 */
@Service
public class OpenAiSttService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSttService.class);

    /** Model mandated by the assignment specification. */
    public static final String MODEL = "gpt-4o-mini-transcribe";

    /** Container formats accepted by the OpenAI transcription API. */
    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("flac", "m4a", "mp3", "mp4", "mpeg", "mpga", "oga", "ogg", "wav", "webm");

    private final RestClient openAiRestClient;
    private final TokenUsageService tokenUsageService;

    public OpenAiSttService(RestClient openAiRestClient, TokenUsageService tokenUsageService) {
        this.openAiRestClient = openAiRestClient;
        this.tokenUsageService = tokenUsageService;
    }

    /**
     * Transcribes the uploaded audio and adds its token usage to the global counters.
     *
     * @param file audio uploaded by a browser or API client
     * @return the transcription text and token usage
     * @throws InvalidAudioException   if the upload is missing, empty or unreadable
     * @throws SpeechToTextException   if OpenAI returns an error or cannot be reached
     */
    public TranscriptionResult transcribe(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAudioException("Please upload a non-empty audio file in the 'file' part.");
        }

        String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
        String filename = "audio." + extension;
        MediaType partType = resolveMediaType(file.getContentType());
        log.info("Transcribing upload '{}' ({}, {} bytes) as {}",
                file.getOriginalFilename(), file.getContentType(), file.getSize(), filename);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidAudioException("Could not read the uploaded audio file.");
        }

        // Build the multipart form with a plain MultiValueMap (written by FormHttpMessageConverter).
        // MultipartBodyBuilder is avoided because it needs reactive-streams, which a servlet app lacks.
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(partType);
        // OpenAI identifies the audio container from the filename extension, so it must match the bytes.
        fileHeaders.setContentDispositionFormData("file", filename);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(new NamedByteArrayResource(bytes, filename), fileHeaders));
        body.add("model", MODEL);
        body.add("response_format", "json");

        Map<?, ?> response;
        try {
            response = openAiRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            // The upstream body describes the problem (e.g. unsupported format) and never contains our key.
            log.warn("OpenAI returned HTTP {} for {} ({} bytes): {}",
                    e.getStatusCode().value(), filename, bytes.length, e.getResponseBodyAsString());
            throw new SpeechToTextException("Speech-to-text service rejected the request (HTTP "
                    + e.getStatusCode().value() + ")" + upstreamMessage(e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            log.warn("Could not reach OpenAI: {}", e.toString());
            throw new SpeechToTextException("Speech-to-text service is unavailable: " + e.getClass().getSimpleName(), e);
        }

        TranscriptionResult result = parse(response);
        tokenUsageService.record(result.inputTokens(), result.outputTokens());
        log.debug("Transcribed {} bytes of {}: {} input / {} output tokens",
                bytes.length, extension, result.inputTokens(), result.outputTokens());
        return result;
    }

    /** Extracts text and token usage from the OpenAI JSON response. */
    static TranscriptionResult parse(Map<?, ?> response) {
        if (response == null) {
            return new TranscriptionResult("", 0, 0);
        }
        Object text = response.get("text");
        long input = 0;
        long output = 0;
        if (response.get("usage") instanceof Map<?, ?> usage) {
            input = readLong(usage.get("input_tokens"));
            output = readLong(usage.get("output_tokens"));
        }
        return new TranscriptionResult(text == null ? "" : text.toString(), input, output);
    }

    /**
     * Chooses a file extension that matches the real audio container.
     *
     * <p>The part's content type is preferred (browsers set it from MediaRecorder's mime type);
     * the original filename extension is used for API clients that send a generic content type.
     */
    static String resolveExtension(String contentType, String originalFilename) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("webm")) return "webm";
        if (type.contains("ogg")) return "ogg";
        if (type.contains("mp4") || type.contains("m4a") || type.contains("aac")) return "mp4";
        if (type.contains("mpeg") || type.contains("mp3")) return "mp3";
        if (type.contains("wav")) return "wav";
        if (type.contains("flac")) return "flac";

        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                String ext = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (SUPPORTED_EXTENSIONS.contains(ext)) {
                    return ext;
                }
            }
        }
        // MediaRecorder in Chromium-based browsers defaults to webm.
        return "webm";
    }

    /** Uses the upload's content type without codec parameters, or a generic binary type. */
    static MediaType resolveMediaType(String contentType) {
        try {
            MediaType parsed = MediaType.parseMediaType(contentType);
            return new MediaType(parsed.getType(), parsed.getSubtype());
        } catch (RuntimeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /** Extracts OpenAI's human-readable error message (safe to show; it never contains our key). */
    static String upstreamMessage(String body) {
        if (body == null) {
            return ".";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]{0,200})").matcher(body);
        return m.find() ? ": " + m.group(1) : ".";
    }

    private static long readLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    /** Byte array resource that reports a filename, which multipart encoding requires. */
    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        NamedByteArrayResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
