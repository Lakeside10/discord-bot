package taneltomson.discord.util.web.data;


import java.time.LocalDateTime;

import lombok.Data;
import lombok.NonNull;


@Data
public class PlayedMatch {
    @NonNull private String opponent;
    @NonNull private String matchType;
    @NonNull private LocalDateTime date;
}
