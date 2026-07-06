package com.cuscatlan.coworking.payment;

import com.cuscatlan.coworking.config.properties.PaymentGatewayProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente al servicio externo de validacion de pago. Uso RestClient (sincrono,
 * moderno) porque la app es 100% MVC bloqueante; meter WebClient/Reactor por una
 * sola llamada seria mezclar paradigmas. El "lento" del gateway se controla con
 * el read-timeout del request factory, no con @TimeLimiter (que pide retorno async).
 */
@Component
public class PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);

    private final RestClient restClient;

    public PaymentGatewayClient(PaymentGatewayProperties props) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(props.connectTimeout())
                .withReadTimeout(props.readTimeout());
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(props.baseUrl())
                .build();
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "validateFallback")
    public PaymentValidationResult validate(PaymentRequest request) {
        return restClient.post()
                .uri("/validate")
                .body(request)
                .retrieve()
                .body(PaymentValidationResult.class);
    }

    // firma = argumentos originales + Throwable. Cuando el circuito esta abierto o la
    // llamada falla/tarda, dejamos la reserva PENDING_PAYMENT en vez de botarle un 500
    // al usuario: no se pierde la intencion, se reconcilia despues.
    private PaymentValidationResult validateFallback(PaymentRequest request, Throwable t) {
        log.warn("Gateway de pago no disponible ({}); la reserva {} queda PENDING_PAYMENT",
                t.toString(), request.reservationId());
        return PaymentValidationResult.pending();
    }
}

