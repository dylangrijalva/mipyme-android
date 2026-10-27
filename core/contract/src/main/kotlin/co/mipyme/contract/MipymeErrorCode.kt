package co.mipyme.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MipymeErrorCode {
    @SerialName("internal_server_error")
    INTERNAL_SERVER_ERROR,

    @SerialName("rate_limit_exceeded")
    RATE_LIMIT_EXCEEDED,

    @SerialName("forbidden_error")
    FORBIDDEN_ERROR,

    @SerialName("account_not_provisioned")
    ACCOUNT_NOT_PROVISIONED,

    @SerialName("validation_error")
    VALIDATION_ERROR,

    @SerialName("conflict_error")
    CONFLICT_ERROR,

    @SerialName("resource_not_found")
    RESOURCE_NOT_FOUND,

    @SerialName("invalid_oauth_token")
    INVALID_OAUTH_TOKEN,

    @SerialName("sync_token_invalid")
    SYNC_TOKEN_INVALID,

    @SerialName("sync_unavailable")
    SYNC_UNAVAILABLE,

    @SerialName("concurrent_update_conflict")
    CONCURRENT_UPDATE_CONFLICT,

    @SerialName("idempotency_key_mismatch")
    IDEMPOTENCY_KEY_MISMATCH,

    @SerialName("idempotency_request_in_progress")
    IDEMPOTENCY_REQUEST_IN_PROGRESS,

    @SerialName("idempotency_unavailable")
    IDEMPOTENCY_UNAVAILABLE,

    @SerialName("collection_name_already_exists")
    COLLECTION_NAME_ALREADY_EXISTS,

    @SerialName("sku_already_exists")
    SKU_ALREADY_EXISTS,

    @SerialName("barcode_already_exists")
    BARCODE_ALREADY_EXISTS,

    @SerialName("price_below_unit_cost")
    PRICE_BELOW_UNIT_COST,

    @SerialName("payment_insufficient")
    PAYMENT_INSUFFICIENT,

    @SerialName("stock_insufficient")
    STOCK_INSUFFICIENT,

    @SerialName("location_limit_exceeded")
    LOCATION_LIMIT_EXCEEDED,
}
