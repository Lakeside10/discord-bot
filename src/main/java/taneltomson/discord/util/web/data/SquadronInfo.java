package taneltomson.discord.util.web.data;


import java.time.Duration;

import lombok.Data;
import lombok.NonNull;


@Data
public class SquadronInfo {
    @NonNull private Integer position;
    @NonNull private String shortName;
    @NonNull private String fullName;
    @NonNull private Integer squibPoints;
    @NonNull private Integer memberCount;
    @NonNull private Integer airKills;
    @NonNull private Integer groundKills;
    @NonNull private Integer deaths;
    @NonNull private Duration squibPlayTime;
}
