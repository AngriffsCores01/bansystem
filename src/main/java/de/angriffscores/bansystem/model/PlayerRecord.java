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
public class PlayerRecord {

    private @NonNull UUID uuid;
    private @NonNull String name;
    private @Nullable Instant lastSeen;
}
