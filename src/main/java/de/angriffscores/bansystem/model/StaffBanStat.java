package de.angriffscores.bansystem.model;

import java.util.UUID;
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
public class StaffBanStat {
    private @NonNull UUID staffUuid;
    private @NonNull String staffName;
    private long banCount;
}
