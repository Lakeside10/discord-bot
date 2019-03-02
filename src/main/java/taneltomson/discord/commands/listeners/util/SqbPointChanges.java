package taneltomson.discord.commands.listeners.util;


import java.util.ArrayList;
import java.util.List;


public class SqbPointChanges {
    private final List<PointChange> changes = new ArrayList<>();

    public void addChange(PointChange change) {
        changes.add(change);
    }

    public int getNumberOfChanges() {
        return changes.size();
    }

    public int getNumberOfWins() {
        // Integer division on purpose - need to have 8 players gain points to have won.
        return Math.toIntExact(changes.stream()
                                      .filter(PointChange::gainedPoints)
                                      .count()) / 8;
    }

    public int getNumberOfLosses() {
        final int pointLosses = Math.toIntExact(changes.stream()
                                                       .filter(PointChange::lostPoints)
                                                       .count());

        // Ceil the division in case we have people with 0 points in the losing platoon - they of
        // course lose no points.
        return ((Double) Math.ceil(pointLosses / 8.0)).intValue();
    }
}
