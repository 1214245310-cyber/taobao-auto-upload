package com.loa.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() == null ? "参数错误" : e.getBindingResult().getFieldError().getDefaultMessage();
        return R.fail(400, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegal(IllegalArgumentException e) { return R.fail(400, e.getMessage()); }

    @ExceptionHandler(Exception.class)
    public R<Void> handle(Exception e) {
        String msg = (e.getMessage() == null || e.getMessage().isBlank()) ? "服务异常" : e.getMessage();
        return R.fail(msg);
    }
}
