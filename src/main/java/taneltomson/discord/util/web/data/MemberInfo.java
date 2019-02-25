package taneltomson.discord.util.web.data;


import java.time.LocalDate;
import java.util.Arrays;

import lombok.Data;
import lombok.NonNull;


@Data
public class MemberInfo {
    @NonNull private String name;
    @NonNull private Integer squibsPoints;
    @NonNull private String role; // TODO: Enum
    @NonNull private LocalDate joinDate;

    public String getDiscordEscapedName() {
        final StringBuilder sb = new StringBuilder();

        for (char c : name.toCharArray()) {
            if (Arrays.asList('_', '*', '~').contains(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }

        return sb.toString();
    }
}
