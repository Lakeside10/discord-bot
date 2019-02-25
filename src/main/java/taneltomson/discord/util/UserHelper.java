package taneltomson.discord.util;


import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;


@Slf4j
public class UserHelper extends RequestHelper {
    public static void addRoleToUser(IUser user, IRole role) {
        queueRequest(() -> user.addRole(role));
        log.debug("addRoleToUser - Added role: {} to user: {}", role.getName(), user.getName());
    }

    public static void removeRoleFromUser(IUser user, IRole role) {
        queueRequest(() -> user.removeRole(role));
        log.debug("removeRoleFromUser - Removed role: {} from user: {}",
                  role.getName(), user.getName());
    }
}
