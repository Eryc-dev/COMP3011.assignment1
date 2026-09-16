package comp3011.assignment1.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.dto.TranscriptionResponse;
import comp3011.assignment1.service.OpenAiSttService;

/** Receives audio from browser clients and returns its transcription. */
@RestController
@RequestMapping("/api/v1")
public class TranscriptionController {

    private final OpenAiSttService sttService;

    public TranscriptionController(OpenAiSttService sttService) {
        this.sttService = sttService;
    }

    /**
     * Transcribes an uploaded recording.
     *
     * @param file multipart part named {@code file} containing webm/ogg/mp4/mp3/wav audio
     * @return JSON {@code {"text": "..."}}
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscriptionResponse transcribe(@RequestParam("file") MultipartFile file) {
        return new TranscriptionResponse(sttService.transcribe(file).text());
    }
}
