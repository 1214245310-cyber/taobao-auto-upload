package com.loa.common;

/**
 * 未登录 / 登录已过期。
 * 单独一个异常类型，便于全局处理返回 401，
 * 前端据此清 token 并跳登录页（而不是只弹一条红字留在后台）。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
