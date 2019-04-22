package jp.gr.java_conf.uzresk.samples.ecs;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Configuration
public class AppConfig {

    private static final String PROTOCOL = "AJP/1.3";

    @Value("${tomcat.ajp.port}") //Defined on application.properties
    private int ajpPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return server ->
                Optional.ofNullable(server)
                        .ifPresent(s -> s.addAdditionalTomcatConnectors(redirectConnector()));
    }

    private Connector redirectConnector() {

        String tomcatAjpPort = System.getenv("TOMCAT_AJP_PORT");
        if (!StringUtils.isEmpty(tomcatAjpPort)) {
            ajpPort = Integer.parseInt(tomcatAjpPort);
        }
        Connector connector = new Connector(PROTOCOL);
        connector.setScheme("http");
        connector.setPort(ajpPort);
        connector.setRedirectPort(8443);
        connector.setSecure(false);
        connector.setAllowTrace(false);
        return connector;
    }
}
