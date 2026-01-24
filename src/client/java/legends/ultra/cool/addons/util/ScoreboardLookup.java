package legends.ultra.cool.addons.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;

public final class ScoreboardLookup {

    private ScoreboardLookup() {}

    /**
     * Returns true if the sidebar scoreboard contains the given text
     * anywhere (case-insensitive).
     */
    public static boolean sidebarContains(String needle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        Scoreboard scoreboard = client.world.getScoreboard();

        // This is the objective shown on the RIGHT side of the screen
        ScoreboardObjective sidebar =
                scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (sidebar == null) return false;

        String search = needle.toLowerCase();

        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(sidebar)) {
            String line =
                    entry.display() != null
                            ? entry.display().getString()
                            : entry.owner();

            System.out.println(line);

            if (line != null && line.toLowerCase().contains(search)) {
                return true;
            }
        }

        return false;
    }
}
