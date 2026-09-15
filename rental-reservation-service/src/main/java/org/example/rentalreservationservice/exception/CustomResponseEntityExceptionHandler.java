package org.example.rentalreservationservice.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@ControllerAdvice
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorEntity> handleBusiness(BusinessException ex) {
        return new ResponseEntity<>(new ErrorEntity(ex.getMessage(), LocalDate.now()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorEntity> handleMissing(NoSuchElementException ex) {
        return new ResponseEntity<>(new ErrorEntity(ex.getMessage(), LocalDate.now()), HttpStatus.NOT_FOUND);
    }

    /**
     * Mreža za slučajeve koje nismo ručno validirali pre upisa (npr. neka nova CHECK
     * constraint dodata direktno u bazi). Bez ovoga korisnik dobija goli 500 sa
     * SQL stack trace-om umesto razumljive 400 poruke.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorEntity> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        return new ResponseEntity<>(
                new ErrorEntity("Podaci nisu prošli proveru u bazi: " + rootMessage, LocalDate.now()),
                HttpStatus.BAD_REQUEST);
    }
}
