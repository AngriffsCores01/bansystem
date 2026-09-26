package de.angriffscores.bansystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivePunishmentEntry {
    private @NonNull Punishment punishment;
    private @NonNull String targetName;
}
