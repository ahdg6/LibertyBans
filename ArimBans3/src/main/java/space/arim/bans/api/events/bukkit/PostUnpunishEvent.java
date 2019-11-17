package space.arim.bans.api.events.bukkit;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.events.UniversalUnpunishEvent;

public class PostUnpunishEvent extends AbstractBukkitEvent implements UniversalUnpunishEvent {

	private final boolean auto;
	
	public PostUnpunishEvent(final Punishment punishment) {
		this(punishment, false);
	}
	
	public PostUnpunishEvent(final Punishment punishment, final boolean auto) {
		super(punishment);
		this.auto = auto;
	}

	@Override
	public boolean isAutomatic() {
		return auto;
	}
	
}
