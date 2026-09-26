package de.angriffscores.bansystem.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Punishment {

    private long id;
    private @NonNull UUID targetUuid;
    private @NonNull PunishmentType type;
    private @NonNull String reason;
    private @NonNull UUID staffUuid;
    private @NonNull String staffName;
    private @NonNull Instant createdAt;
    private @Nullable Instant expiresAt;
    private boolean active;
    private @Nullable Instant revokedAt;
    private @Nullable UUID revokedByUuid;
    private @Nullable String revokedByName;

    /**
     * @return whether this punishment is currently in effect
     */
    public boolean isCurrentlyActive() {
        if (!this.active) {
            return false;
        }
        if (this.expiresAt == null) {
            return true;
        }
        return this.expiresAt.isAfter(Instant.now());
    }

    /**
     * @return whether this punishment has no expiry
     */
    public boolean isPermanent() {
        return this.expiresAt == null;
    }
}
