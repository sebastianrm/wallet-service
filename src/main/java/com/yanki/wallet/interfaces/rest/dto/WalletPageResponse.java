package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response paginado para la lista de wallets.
 *
 * <p>Contiene la página actual, tamaño, contenido, total de elementos
 * y total de páginas, así como indicadores de primera y última página.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "WalletPageResponse", description = "Respuesta paginada para la lista de wallets con metadatos de paginación.")
public class WalletPageResponse {

    @Schema(description = "Lista de wallets en la página actual.")
    private List<WalletResponse> content;

    @Schema(description = "Número de página actual (base 0).", example = "0")
    private int page;

    @Schema(description = "Tamaño de la página (número de elementos por página).", example = "20")
    private int size;

    @Schema(description = "Total de wallets disponibles en todas las páginas.", example = "150")
    private long totalElements;

    @Schema(description = "Total de páginas disponibles.", example = "8")
    private int totalPages;

    @Schema(description = "Indica si es la primera página.", example = "true")
    private boolean first;

    @Schema(description = "Indica si es la última página.", example = "false")
    private boolean last;
}
