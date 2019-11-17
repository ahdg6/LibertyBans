package space.arim.bans.api.events.bukkit;

import org.bukkit.event.Cancellable;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.events.UniversalPunishEvent;

public class PunishEvent extends AbstractBukkitEvent implements UniversalPunishEvent, Cancellable {

	private final boolean retro;
	
	private boolean cancel = false;
	
	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}
	
	@Override
	public boolean isCancelled() {
		return this.cancel;
	}
	
	public PunishEvent(final Punishment punishment) {
		this(punishment, false);
	}
	
	public PunishEvent(final Punishment punishment, boolean retro) {
		super(punishment);
		this.retro = retro;
	}
	
	@Override
	public boolean isRetrogade() {
		return retro;
	}

}
