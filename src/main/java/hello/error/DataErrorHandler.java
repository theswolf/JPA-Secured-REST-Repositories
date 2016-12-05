package hello.error;

import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.NestedServletException;

import javax.validation.ValidationException;

/**
 * Created by christian on 23/11/16.
 */
@ControllerAdvice(basePackageClasses = RepositoryRestExceptionHandler.class)
class DataErrorHandler {
    @ExceptionHandler
    ResponseEntity handle(Exception e) {
        e.printStackTrace();
        Throwable root = isFromException(e,ValidationException.class);
        if(root != null) {
            return new ResponseEntity(root.getMessage(), new HttpHeaders(), HttpStatus.FORBIDDEN);
        }

        else {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private <T> Throwable isFromException(Throwable input,Class<T> causeClass) {
        if(input == null)
            return null;
        if(input.getClass().equals(causeClass)) {
            return input;
        }
        return isFromException(input.getCause(),causeClass);
    }
}
