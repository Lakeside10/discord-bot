package taneltomson.discord.commands.listeners.util;


import taneltomson.discord.util.web.data.MemberInfo;


public class PointChange {
    private final MemberInfo before;
    private final MemberInfo after;

    public PointChange(MemberInfo before, MemberInfo after) {
        this.before = before;
        this.after = after;
    }

    public boolean gainedPoints() {
        return after.getSquibsPoints() > before.getSquibsPoints();
    }

    public boolean lostPoints() {
        return after.getSquibsPoints() < before.getSquibsPoints();
    }
}
