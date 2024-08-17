package com.ziio.buddylink.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

@Component
@Slf4j
public class HttpSessionConfig extends ServerEndpointConfig.Configurator implements ServletRequestListener {

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpSession session = ((HttpServletRequest) sre.getServletRequest()).getSession();
        log.info("HttpSession initialized: {}", session.getId());
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        if (httpSession != null) {
            sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
            log.info("修改握手中设置的HttpSession: {}", httpSession.getId());
        } else {
            log.error("修改握手中的HttpSession为空");
        }
        super.modifyHandshake(sec, request, response);
    }
}
