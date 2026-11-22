package com.yanki.wallet.interfaces.rest.controller;

import com.yanki.wallet.domain.exception.BusinessException;
import com.yanki.wallet.domain.exception.ErrorCodes;
import com.yanki.wallet.domain.model.Transaction;
import com.yanki.wallet.domain.model.Wallet;
import com.yanki.wallet.domain.port.in.AssociateCardUseCase;
import com.yanki.wallet.domain.port.in.DisassociateCardUseCase;
import com.yanki.wallet.domain.port.in.GetWalletUseCase;
import com.yanki.wallet.domain.port.in.OpenWalletUseCase;
import com.yanki.wallet.domain.port.in.SendP2PUseCase;
import com.yanki.wallet.domain.port.in.TopUpWalletUseCase;
import com.yanki.wallet.domain.port.in.WithdrawWalletUseCase;
import com.yanki.wallet.interfaces.rest.dto.ApiError;
import com.yanki.wallet.interfaces.rest.dto.AmountRequest;
import com.yanki.wallet.interfaces.rest.dto.AssociateCardRequest;
import com.yanki.wallet.interfaces.rest.dto.OpenWalletRequest;
import com.yanki.wallet.interfaces.rest.dto.SendRequest;
import com.yanki.wallet.interfaces.rest.dto.TransactionResponse;
import com.yanki.wallet.interfaces.rest.dto.WalletPageResponse;
import com.yanki.wallet.interfaces.rest.dto.WalletResponse;
import com.yanki.wallet.interfaces.rest.mapper.WalletRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controlador REST reactivo para operaciones sobre wallets Yanki.
 *
 * <p>Endpoints resourceful para el monedero móvil:</p>
 * <ul>
 *   <li>POST /api/wallets - Abrir wallet</li>
 *   <li>GET /api/wallets/{id} - Obtener wallet por ID</li>
 *   <li>GET /api/wallets/phone/{phone} - Buscar wallet por teléfono</li>
 *   <li>POST /api/wallets/{id}/topup - Cargar saldo (top-up)</li>
 *   <li>POST /api/wallets/{id}/send - Enviar dinero P2P</li>
 *   <li>POST /api/wallets/{id}/withdraw - Retirar saldo a cuenta bancaria</li>
 *   <li>POST /api/wallets/{id}/associate-card - Asociar tarjeta débito</li>
 *   <li>POST /api/wallets/{id}/disassociate-card - Desasociar tarjeta débito</li>
 *   <li>GET /api/wallets/{id}/transactions - Historial de transacciones</li>
 * </ul>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Wallets", description = "Gestión de monederos móviles Yanki: apertura, top-up, P2P, retiro y asociación de tarjeta débito")
public class WalletController {

    private final OpenWalletUseCase openWalletUseCase;
    private final TopUpWalletUseCase topUpWalletUseCase;
    private final SendP2PUseCase sendP2PUseCase;
    private final WithdrawWalletUseCase withdrawWalletUseCase;
    private final AssociateCardUseCase associateCardUseCase;
    private final DisassociateCardUseCase disassociateCardUseCase;
    private final GetWalletUseCase getWalletUseCase;

    public WalletController(
            OpenWalletUseCase openWalletUseCase,
            TopUpWalletUseCase topUpWalletUseCase,
            SendP2PUseCase sendP2PUseCase,
            WithdrawWalletUseCase withdrawWalletUseCase,
            AssociateCardUseCase associateCardUseCase,
            DisassociateCardUseCase disassociateCardUseCase,
            GetWalletUseCase getWalletUseCase
    ) {
        this.openWalletUseCase = openWalletUseCase;
        this.topUpWalletUseCase = topUpWalletUseCase;
        this.sendP2PUseCase = sendP2PUseCase;
        this.withdrawWalletUseCase = withdrawWalletUseCase;
        this.associateCardUseCase = associateCardUseCase;
        this.disassociateCardUseCase = disassociateCardUseCase;
        this.getWalletUseCase = getWalletUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Abre una nueva wallet",
        description = "Crea un monedero móvil Yanki para un cliente identificado por DNI/CEX/Pasaporte. " +
            "No se requiere ser cliente del banco, solo se necesita documento de identidad, número de celular, " +
            "IMEI del celular y correo electrónico. Valida que no exista ya una wallet para el mismo cliente " +
            "y que el cliente no tenga deuda vencida. Se publica el evento wallet.opened.v1."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Wallet creada exitosamente. Se genera un ID único y el estado inicial es ACTIVE.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de solicitud inválidos. Verifique los campos obligatorios: documentType, documentNumber, phone, imei, email.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Regla de negocio violada. Por ejemplo: wallet ya existe para ese cliente o el cliente tiene deuda vencida.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Datos para abrir una nueva wallet Yanki.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = OpenWalletRequest.class),
            examples = {
                @ExampleObject(name = "Apertura wallet personal", summary = "Apertura de wallet con DNI",
                    value = "{\"documentType\":\"DNI\",\"documentNumber\":\"70100001\",\"phone\":\"+51999888777\",\"imei\":\"353456789012345\",\"email\":\"juan@email.com\",\"firstName\":\"Juan\",\"lastName\":\"Pérez\",\"birthDate\":\"1990-01-01\"}")
            }
        )
    )
    public Mono<WalletResponse> openWallet(@Valid @RequestBody OpenWalletRequest request) {
        String customerId = request.documentType() + "-" + request.documentNumber();
        log.info("POST /api/wallets - Apertura de wallet: customerId={} phone={} imei={}",
                customerId, request.phone(), request.imei());
        log.debug("Request body: documentType={} documentNumber={} email={}",
                request.documentType(), request.documentNumber(), request.email());
        try {
            Mono<WalletResponse> response = RxJava3Adapter.singleToMono(
                    openWalletUseCase.execute(customerId, request.phone(), request.imei())
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .doOnSuccess(w -> log.info("POST /api/wallets - Wallet creada: walletId={}", w.id()))
                    .doOnError(error -> log.error("POST /api/wallets - Error al crear wallet: customerId={} - {}",
                            customerId, error.getMessage()));
        } catch (Exception e) {
            log.error("POST /api/wallets - Excepción al crear wallet: customerId={} - {}",
                    customerId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtiene detalle de una wallet",
        description = "Retorna la información completa de una wallet identificada por su ID único, " +
            "incluyendo saldo actual, estado, número de teléfono y tarjeta débito asociada."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Wallet encontrada con todos sus detalles.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró una wallet con el ID proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    public Mono<WalletResponse> getWalletById(
            @Parameter(description = "Identificador único de la wallet.", example = "WALLET-1234567890ABCDEF", required = true)
            @PathVariable String id) {
        log.info("GET /api/wallets/{} - Consulta de wallet por ID", id);
        try {
            Mono<WalletResponse> response = RxJava3Adapter.maybeToMono(
                    getWalletUseCase.findById(id)
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .switchIfEmpty(Mono.error(new BusinessException(
                            ErrorCodes.WALLET_NOT_FOUND,
                            "Wallet no encontrada: " + id)))
                    .doOnSuccess(w -> log.debug("GET /api/wallets/{} - Wallet encontrada: saldo={}", id, w.balance()))
                    .doOnError(error -> log.error("GET /api/wallets/{} - Error al consultar wallet: {}", id, error.getMessage()));
        } catch (Exception e) {
            log.error("GET /api/wallets/{} - Excepción al consultar wallet: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/phone/{phone}")
    @Operation(
        summary = "Busca wallet por número de teléfono",
        description = "Retorna la wallet asociada a un número de teléfono. " +
            "Este endpoint se usa principalmente para identificar el wallet destino en operaciones P2P."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Wallet encontrada para el número de teléfono proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró una wallet asociada al número de teléfono proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    public Mono<WalletResponse> getWalletByPhone(
            @Parameter(description = "Número de teléfono asociado a la wallet.", example = "+51999888777", required = true)
            @PathVariable String phone) {
        log.info("GET /api/wallets/phone/{} - Búsqueda de wallet por teléfono", phone);
        try {
            Mono<WalletResponse> response = RxJava3Adapter.maybeToMono(
                    getWalletUseCase.findByPhoneNumber(phone)
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .switchIfEmpty(Mono.error(new BusinessException(
                            ErrorCodes.WALLET_NOT_FOUND,
                            "Wallet no encontrada para el teléfono: " + phone)))
                    .doOnSuccess(w -> log.debug("GET /api/wallets/phone/{} - Wallet encontrada: walletId={}", phone, w.id()))
                    .doOnError(error -> log.error("GET /api/wallets/phone/{} - Error al buscar wallet: {}", phone, error.getMessage()));
        } catch (Exception e) {
            log.error("GET /api/wallets/phone/{} - Excepción al buscar wallet: {}", phone, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping
    @Operation(
        summary = "Lista todas las wallets con paginación",
        description = "Retorna una lista paginada de wallets Yanki. " +
            "Si no se proporcionan parámetros, retorna todas las wallets."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de wallets encontradas con metadatos de paginación.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class))
        )
    })
    public Mono<WalletPageResponse> findAll(
            @Parameter(description = "Número de página (base 0).", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (máximo 100).", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.debug("GET /api/wallets - Listar wallets: page={}, size={}", page, size);
        try {
            if (size > 100) {
                size = 100;
            }
            int pageSize = size;
            java.util.List<Wallet> wallets = new java.util.ArrayList<>();
            getWalletUseCase.listAll().subscribe(wallets::add);
            java.util.List<WalletResponse> content = wallets.stream()
                .map(WalletRestMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
            int totalElements = content.size();
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            int fromIndex = page * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, totalElements);
            java.util.List<WalletResponse> pagedContent = (fromIndex < totalElements)
                ? content.subList(fromIndex, toIndex)
                : java.util.Collections.emptyList();
            WalletPageResponse response = WalletPageResponse.builder()
                .content(pagedContent)
                .page(page)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
            log.debug("GET /api/wallets - Wallets encontradas: {} resultados", totalElements);
            return Mono.just(response)
                .doOnError(error -> log.error("GET /api/wallets - Error al listar wallets: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("GET /api/wallets - Excepción al listar wallets: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{id}/topup")
    @Operation(
        summary = "Carga saldo desde tarjeta débito (top-up)",
        description = "Carga saldo al wallet desde la tarjeta débito asociada. " +
            "Requiere que la wallet tenga una tarjeta débito válida asociada. " +
            "El flujo es event-driven: se publica wallet.topup.requested.v1 y se espera la respuesta vía wallet.topup.settled.v1."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Top-up iniciado exitosamente. La transacción se registra con estado PENDING hasta la conciliación.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró una wallet con el ID proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "La wallet no tiene tarjeta débito asociada o está bloqueada.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Monto a cargar al wallet.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AmountRequest.class),
            examples = {
                @ExampleObject(name = "Top-up de 100 soles",
                    value = "{\"amount\":100.00}")
            }
        )
    )
    public Mono<TransactionResponse> topUp(
            @Parameter(description = "Identificador único de la wallet.", example = "WALLET-1234567890ABCDEF", required = true)
            @PathVariable String id,
            @Valid @RequestBody AmountRequest request) {
        log.info("POST /api/wallets/{}/topup - Top-up solicitado: wallet={} monto={}", id, id, request.amount());
        try {
            Mono<TransactionResponse> response = RxJava3Adapter.singleToMono(
                    topUpWalletUseCase.execute(id, request.amount())
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .doOnSuccess(tx -> log.info("POST /api/wallets/{}/topup - Top-up iniciado: txId={}", id, tx.id()))
                    .doOnError(error -> log.error("POST /api/wallets/{}/topup - Error en top-up: monto={} - {}", id, request.amount(), error.getMessage()));
        } catch (Exception e) {
            log.error("POST /api/wallets/{}/topup - Excepción en top-up: monto={} - {}", id, request.amount(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{id}/send")
    @Operation(
        summary = "Envía dinero a otro wallet P2P",
        description = "Transfiere saldo entre wallets Yanki identificadas por número de teléfono. " +
            "Valida saldo suficiente, que el destinatario exista y que no se envíe a uno mismo. " +
            "Se publica el evento wallet.transaction.registered.v1."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Envío P2P realizado exitosamente. El saldo se debita del origen y acredita al destino.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró la wallet origen.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Saldo insuficiente, wallet bloqueada, destinatario no encontrado o intento de envío a uno mismo.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Datos del envío P2P: teléfono del destinatario y monto.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SendRequest.class),
            examples = {
                @ExampleObject(name = "Envío P2P de 50 soles",
                    value = "{\"recipientPhone\":\"+51999888777\",\"amount\":50.00}")
            }
        )
    )
    public Mono<TransactionResponse> send(
            @Parameter(description = "Identificador único de la wallet origen.", example = "WALLET-1234567890ABCDEF", required = true)
            @PathVariable String id,
            @Valid @RequestBody SendRequest request) {
        log.info("POST /api/wallets/{}/send - Envío P2P: wallet={} destinatario={} monto={}",
                id, id, request.recipientPhone(), request.amount());
        try {
            Mono<TransactionResponse> response = RxJava3Adapter.singleToMono(
                    sendP2PUseCase.execute(id, request.recipientPhone(), request.amount())
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .doOnSuccess(tx -> log.info("POST /api/wallets/{}/send - Envío P2P completado: txId={}", id, tx.id()))
                    .doOnError(error -> log.error("POST /api/wallets/{}/send - Error en envío P2P: destinatario={} monto={} - {}",
                            id, request.recipientPhone(), request.amount(), error.getMessage()));
        } catch (Exception e) {
            log.error("POST /api/wallets/{}/send - Excepción en envío P2P: destinatario={} monto={} - {}",
                    id, request.recipientPhone(), request.amount(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{id}/withdraw")
    @Operation(
        summary = "Retira saldo a cuenta bancaria",
        description = "Transfiere saldo del wallet a una cuenta bancaria del mismo cliente. " +
            "Requiere que el cliente tenga una cuenta bancaria válida asociada. " +
            "Se publica el evento wallet.transaction.registered.v1."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Retiro realizado exitosamente. El saldo se debita del wallet y acredita a la cuenta bancaria.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró una wallet con el ID proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Saldo insuficiente, wallet bloqueada o el cliente no tiene cuenta bancaria asociada.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Monto a retirar del wallet.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AmountRequest.class),
            examples = {
                @ExampleObject(name = "Retiro de 200 soles",
                    value = "{\"amount\":200.00}")
            }
        )
    )
    public Mono<TransactionResponse> withdraw(
            @Parameter(description = "Identificador único de la wallet.", example = "WALLET-1234567890ABCDEF", required = true)
            @PathVariable String id,
            @Valid @RequestBody AmountRequest request) {
        log.info("POST /api/wallets/{}/withdraw - Retiro solicitado: wallet={} monto={}", id, id, request.amount());
        try {
            Mono<TransactionResponse> response = RxJava3Adapter.singleToMono(
                    withdrawWalletUseCase.withdraw(id, request.amount())
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .doOnSuccess(tx -> log.info("POST /api/wallets/{}/withdraw - Retiro completado: txId={}", id, tx.id()))
                    .doOnError(error -> log.error("POST /api/wallets/{}/withdraw - Error en retiro: monto={} - {}", id, request.amount(), error.getMessage()));
        } catch (Exception e) {
            log.error("POST /api/wallets/{}/withdraw - Excepción en retiro: monto={} - {}", id, request.amount(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{id}/associate-card")
    @Operation(
        summary = "Asocia tarjeta débito al wallet",
        description = "Vincula una tarjeta débito del banco al wallet para permitir operaciones de top-up. " +
            "El saldo se carga o acredita solo a la cuenta principal asociada a la tarjeta débito."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tarjeta débito asociada exitosamente al wallet.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró una wallet con el ID proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "La wallet ya tiene una tarjeta débito asociada o la wallet está bloqueada.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Identificador de la tarjeta débito a asociar.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AssociateCardRequest.class),
            examples = {
                @ExampleObject(name = "Asociar tarjeta débito",
                    value = "{\"cardId\":\"CARD-1234567890ABCDEF\"}")
            }
        )
    )
    public Mono<WalletResponse> associateCard(
            @Parameter(description = "Identificador único de la wallet.", example = "WALLET-1234567890ABCDEF", required = true)
            @PathVariable String id,
            @Valid @RequestBody AssociateCardRequest request) {
        log.info("POST /api/wallets/{}/associate-card - Asociar tarjeta: wallet={} cardId={}", id, id, request.cardId());
        try {
            Mono<WalletResponse> response = RxJava3Adapter.singleToMono(
                    associateCardUseCase.execute(id, request.cardId())
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .doOnSuccess(w -> log.info("POST /api/wallets/{}/associate-card - Tarjeta asociada: cardId={}", id, request.cardId()))
                    .doOnError(error -> log.error("POST /api/wallets/{}/associate-card - Error al asociar tarjeta: cardId={} - {}",
                            id, request.cardId(), error.getMessage()));
        } catch (Exception e) {
            log.error("POST /api/wallets/{}/associate-card - Excepción al asociar tarjeta: cardId={} - {}",
                    id, request.cardId(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/{id}/disassociate-card")
    @Operation(
        summary = "Desasocia tarjeta débito del wallet",
        description = "Elimina la vinculación entre la tarjeta débito y el wallet. " +
            "Después de desasociar, el wallet no podrá realizar operaciones de top-up hasta asociar otra tarjeta."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tarjeta débito desasociada exitosamente del wallet.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró una wallet con el ID proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "La wallet no tiene tarjeta débito asociada.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    public Mono<WalletResponse> disassociateCard(
            @Parameter(description = "Identificador único de la wallet.", example = "WALLET-1234567890ABCDEF", required = true)
            @PathVariable String id) {
        log.info("POST /api/wallets/{}/disassociate-card - Desasociar tarjeta: wallet={}", id, id);
        try {
            Mono<WalletResponse> response = RxJava3Adapter.singleToMono(
                    disassociateCardUseCase.execute(id)
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .doOnSuccess(w -> log.info("POST /api/wallets/{}/disassociate-card - Tarjeta desasociada", id))
                    .doOnError(error -> log.error("POST /api/wallets/{}/disassociate-card - Error al desasociar tarjeta: {}", id, error.getMessage()));
        } catch (Exception e) {
            log.error("POST /api/wallets/{}/disassociate-card - Excepción al desasociar tarjeta: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}/transactions")
    @Operation(
        summary = "Historial de transacciones del wallet",
        description = "Retorna el historial completo de transacciones de una wallet específica. " +
            "Incluye operaciones de top-up, envío P2P, recepción P2P y retiro a cuenta bancaria. " +
            "Cada transacción incluye tipo, monto, saldo resultante, estado y fecha de ocurrencia."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de transacciones del wallet.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró una wallet con el ID proporcionado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
        )
    })
    public Flux<TransactionResponse> getTransactions(
            @Parameter(description = "Identificador único de la wallet.", example = "WALLET-1234567890ABCDEF", required = true)
            @PathVariable String id) {
        log.info("GET /api/wallets/{}/transactions - Historial de transacciones: wallet={}", id, id);
        try {
            Flux<TransactionResponse> response = Flux.from(
                    getWalletUseCase.getTransactions(id)
                            .map(WalletRestMapper::toResponse)
            );
            return response
                    .doOnNext(tx -> log.debug("GET /api/wallets/{}/transactions - Transacción: {} tipo: {} monto: {}",
                            id, tx.id(), tx.type(), tx.amount()))
                    .doOnError(error -> log.error("GET /api/wallets/{}/transactions - Error al obtener transacciones: {}", id, error.getMessage()));
        } catch (Exception e) {
            log.error("GET /api/wallets/{}/transactions - Excepción al obtener transacciones: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
