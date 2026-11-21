package com.yanki.wallet.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI 3 (springdoc) para documentación de la API.
 *
 * <p>Define el título, versión, descripción y esquema de seguridad Bearer JWT
 * utilizado en los endpoints REST del servicio wallet-service.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yanki Wallet Service API")
                        .version("1.0.0")
                        .description(
                                "API REST para la gestión de monederos móviles Yanki.\n\n"
                                + "**Funcionalidades principales:**\n"
                                + "- Apertura de wallet sin necesidad de ser cliente del banco\n"
                                + "- Carga de saldo (top-up) desde tarjeta débito vía flujo event-driven con Kafka\n"
                                + "- Envío y recepción P2P entre wallets identificadas por número de teléfono\n"
                                + "- Retiro de saldo a cuenta bancaria del mismo cliente\n"
                                + "- Asociación y desasociación de tarjeta débito\n"
                                + "- Historial de transacciones del wallet\n\n"
                                + "**Requisitos de apertura:**\n"
                                + "- Documento de identidad (DNI/CEX/Pasaporte)\n"
                                + "- Número de celular\n"
                                + "- IMEI del celular (15 dígitos)\n"
                                + "- Correo electrónico\n\n"
                                + "**Reglas de negocio:**\n"
                                + "- Un wallet por cliente (customerId único)\n"
                                + "- Top-up solo con tarjeta débito asociada\n"
                                + "- Bloqueo automático por deuda vencida\n"
                                + "- Saldo máximo configurable (default: 10,000 PEN)\n"
                                + "- Límite diario P2P configurable (default: 2,000 PEN)\n\n"
                                + "**Moneda:** PEN (Sol peruano)\n\n"
                                + "**Seguridad:** Todos los endpoints requieren autenticación Bearer JWT."
                        )
                        .contact(new Contact()
                                .name("Equipo Yanki Platform")
                                .email("semorale@emeal.nttdata.com")
                                .url("https://yanki.bank"))
                        .license(new License().name("Proprietary").url("https://yanki.bank/license")))
                .addTagsItem(new Tag()
                        .name("Wallets")
                        .description(
                                "Operaciones de gestión del monedero móvil Yanki.\n\n"
                                + "Incluye apertura, consulta, top-up, envío P2P, retiro, asociación de tarjeta débito\n"
                                + "y consulta de historial de transacciones.\n"
                                + "La comunicación con otros microservicios es 100% event-driven vía Kafka."
                        ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT obtenido del endpoint de autenticación.")));
    }
}
