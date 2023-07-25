package de.petropia.farmworld;

import de.petropia.spacelifeCore.scoreboard.element.GlobalScoreboardElement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WorldDeleteScoreboardElement extends GlobalScoreboardElement {
    @Override
    public List<String> getContent(Player player) {
        return List.of(Farmworld.convertUnixTimestamp(Farmworld.getNextDelete().getEpochSecond()));
    }

    @Override
    public String getTitle() {
        return "Nächster Reset";
    }

    @Override
    public @Nullable String getPermission() {
        return null;
    }

    @Override
    public int getPriority() {
        return 250;
    }
}
