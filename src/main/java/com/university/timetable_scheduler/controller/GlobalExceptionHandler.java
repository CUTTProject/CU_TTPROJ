package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String messages = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        BaseResponse response = new BaseResponse();
        response.setResponseCode(HttpStatus.BAD_REQUEST.toString());
        response.setError(true);
        response.setPath(request.getRequestURI());
        response.setResponseMessage(messages);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        BaseResponse response = new BaseResponse();
        response.setResponseCode(HttpStatus.BAD_REQUEST.toString());
        response.setError(true);
        response.setPath(request.getRequestURI());
        response.setResponseMessage("Invalid request body: " + ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        BaseResponse response = new BaseResponse();
        response.setResponseCode(HttpStatus.BAD_REQUEST.toString());
        response.setError(true);
        response.setPath(request.getRequestURI());
        response.setResponseMessage("Missing parameter: " + ex.getParameterName());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BaseResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {

        BaseResponse response = new BaseResponse();
        response.setResponseCode(ex.getStatusCode().toString());
        response.setError(true);
        response.setPath(request.getRequestURI());
        response.setResponseMessage(ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {

        // Log the detail server-side; do not echo ex.getMessage() to the caller, which would
        // leak SQL, constraint names and other internals on any unhandled failure.
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);

        BaseResponse response = new BaseResponse();
        response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
        response.setError(true);
        response.setPath(request.getRequestURI());
        response.setResponseMessage("An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}