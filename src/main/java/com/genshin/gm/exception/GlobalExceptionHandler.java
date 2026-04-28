package com.genshin.gm.exception;

import com.genshin.gm.model.OpenCommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理REST客户端异常（如连接失败、超时等）
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<OpenCommandResponse> handleRestClientException(RestClientException e) {
        logger.error("REST客户端异常: {}", e.getMessage(), e);

        OpenCommandResponse response = new OpenCommandResponse();
        response.setRetcode(500);
        response.setMessage("服务器连接失败: " + e.getMessage());

        return ResponseEntity.ok(response);
    }

    /**
     * 处理数字格式异常
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<OpenCommandResponse> handleNumberFormatException(NumberFormatException e) {
        logger.error("数字格式错误: {}", e.getMessage(), e);

        OpenCommandResponse response = new OpenCommandResponse();
        response.setRetcode(400);
        response.setMessage("参数格式错误: " + e.getMessage());

        return ResponseEntity.ok(response);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<OpenCommandResponse> handleNullPointerException(NullPointerException e) {
        logger.error("空指针异常: {}", e.getMessage(), e);

        OpenCommandResponse response = new OpenCommandResponse();
        response.setRetcode(500);
        response.setMessage("服务器内部错误: 缺少必要参数");

        return ResponseEntity.ok(response);
    }

    /**
     * 处理所有其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<OpenCommandResponse> handleException(Exception e) {
        logger.error("未处理的异常: {}", e.getMessage(), e);

        OpenCommandResponse response = new OpenCommandResponse();
        response.setRetcode(500);
        response.setMessage("服务器内部错误: " + e.getMessage());

        return ResponseEntity.ok(response);
    }
}
