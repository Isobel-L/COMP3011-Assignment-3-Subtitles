
package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception) {

        return ResponseEntity.badRequest().body(
                Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleLargeUpload(
            MaxUploadSizeExceededException exception) {

        return ResponseEntity.status(
                HttpStatus.PAYLOAD_TOO_LARGE).body(
                Map.of("error", "Audio file exceeds the upload limit."));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleWhisperFailure(
            RestClientException exception) {

        return ResponseEntity.status(
                HttpStatus.BAD_GATEWAY).body(
                Map.of("error", "Whisper transcription service failed."));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleFileError(
            IOException exception) {

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Unable to process the uploaded audio."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(
            Exception exception) {

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "An unexpected server error occurred."));
    }
}
