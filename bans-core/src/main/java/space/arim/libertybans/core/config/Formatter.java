/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.config;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.api.chat.SendableMessage;
import space.arim.api.configure.ConfigAccessor;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.MiscUtil;

public class Formatter {

	private final LibertyBansCore core;
	
	private static final Entry<String, Long>[] timeUnits;

	private static final long MARGIN_OF_INITIATION = 10; // seconds
	
	public Formatter(LibertyBansCore core) {
		this.core = core;
	}
	
	public SendableMessage parseMessage(String rawMessage) {
		SendableMessage message = parseMessageNoPrefix(rawMessage);
		ConfigAccessor messages = core.getConfigs().getMessages();
		if (messages.getBoolean("all.prefix.enable")) {
			SendableMessage prefix = messages.getObject("all.prefix.value", SendableMessage.class);
			message = prefix.concatenate(message);
		}
		return message;
	}
	
	private SendableMessage parseMessageNoPrefix(String rawMessage) {
		return ConfigUtil.parseMessage(isJsonEnabled(), rawMessage);
	}
	
	public boolean isJsonEnabled() {
		return core.getConfigs().getMessages().getBoolean("formatting.enable-json");
	}
	
	/**
	 * Gets, formats, and parses the punishment message for a punishment. Punishment messages
	 * do not use the global prefix.
	 * 
	 * @param punishment the punishment
	 * @return a future yielding the formatted sendable message
	 */
	public CentralisedFuture<SendableMessage> getPunishmentMessage(Punishment punishment) {
		CentralisedFuture<String> futureVictimFormatted = formatVictim(punishment.getVictim());
		CentralisedFuture<String> futureOperatorFormatted = formatOperator(punishment.getOperator());

		return futureVictimFormatted.thenCombineAsync(futureOperatorFormatted, (victimFormatted, operatorFormatted) -> {
			String path = "additions." + punishment.getType().getLowercaseNamePlural() + ".layout";
			String message = core.getConfigs().getMessages().getString(path);
			return parseMessageNoPrefix(formatWithPunishment0(message, punishment, victimFormatted, operatorFormatted));
		});
	}
	
	private String formatWithPunishment0(String message, Punishment punishment, String victimFormatted,
			String operatorFormatted) {

		final long now = MiscUtil.currentTime();
		final long start = punishment.getStart();
		final long end = punishment.getEnd();

		long timePassed = now - start;

		String durationFormatted;
		String timeEndRelFormatted;

		if (end == 0L) {
			// Permanent punishment
			ConfigAccessor config = core.getConfigs().getMessages();
			durationFormatted = config.getString("formatting.permanent-display.relative");
			timeEndRelFormatted = config.getString("formatting.permanent-display.absolute");
		} else {
			// Temporary punishment
			long duration = end - start;
			durationFormatted = formatRelative(duration);

			long timeRemaining;
			// Using a margin of initiation prevents the "29 days, 23 hours, 59 minutes" issue
			if (timePassed < MARGIN_OF_INITIATION) {
				timeRemaining = duration;
			} else {
				timeRemaining = end - now;
			}
			timeEndRelFormatted = formatRelative(timeRemaining);
		}

		return message
				.replace("%ID%", Integer.toString(punishment.getID()))
				.replace("%TYPE%", formatType(punishment.getType()))
				.replace("%VICTIM%", victimFormatted)
				.replace("%VICTIM_ID%", formatVictimId(punishment.getVictim()))
				.replace("%OPERATOR%", operatorFormatted)
				.replace("%REASON%", punishment.getReason())
				.replace("%SCOPE%", formatScope(punishment.getScope()))
				.replace("%DURATION%", durationFormatted)
				.replace("%TIME_START_ABS%", formatAbsolute(start))
				.replace("%TIME_START_REL%", formatRelative(timePassed))
				.replace("%TIME_END_ABS%", formatAbsolute(end))
				.replace("%TIME_END_REL%", timeEndRelFormatted);
	}
	
	private String formatScope(Scope scope) {
		String scopeDisplay = core.getScopeManager().getServer(scope);
		if (scopeDisplay == null) {
			scopeDisplay = core.getConfigs().getMessages().getString("formatting.global-scope-display");
		}
		return scopeDisplay;
	}
	
	/**
	 * Parses and formats a message with a punishment
	 * 
	 * @param message the message
	 * @param punishment the punishment
	 * @return a future of the resulting formatted sendable message
	 */
	public CentralisedFuture<SendableMessage> formatWithPunishment(String message, Punishment punishment) {
		CentralisedFuture<String> futureVictimFormatted = formatVictim(punishment.getVictim());
		CentralisedFuture<String> futureOperatorFormatted = formatOperator(punishment.getOperator());

		return futureVictimFormatted.thenCombineAsync(futureOperatorFormatted, (victimFormatted, operatorFormatted) -> {
			return parseMessage(formatWithPunishment0(message, punishment, victimFormatted, operatorFormatted));
		});
	}

	private String formatType(PunishmentType type) {
		return type.toString();
	}
	
	private String formatVictimId(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			return UUIDUtil.toShortString(((PlayerVictim) victim).getUUID());
		case ADDRESS:
			return formatAddressVictim((AddressVictim) victim);
		default:
			throw new IllegalStateException("Unknown victim type " + victim.getType());
		}
	}
	
	private CentralisedFuture<String> formatVictim(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			/*
			 * This should be a complete future every time we call this ourselves, because of UUIDMaster's fastCache.
			 * However, for API calls, the UUID/name might not be added to the cache.
			 */
			return core.getUUIDMaster().fullLookupName(((PlayerVictim) victim).getUUID());
		case ADDRESS:
			return core.getFuturesFactory().completedFuture(formatAddressVictim((AddressVictim) victim));
		default:
			throw new IllegalStateException("Unknown victim type " + victim.getType());
		}
	}
	
	public CentralisedFuture<String> formatOperator(Operator operator) {
		switch (operator.getType()) {
		case CONSOLE:
			return core.getFuturesFactory().completedFuture(core.getConfigs().getMessages().getString("formatting.console-display"));
		case PLAYER:
			/*
			 * Similarly in #formatVictim, this should be a complete future every time we call this ourselves,
			 * because of UUIDMaster's fastCache.
			 */
			return core.getUUIDMaster().fullLookupName(((PlayerOperator) operator).getUUID());
		default:
			throw new IllegalStateException("Unknown operator type " + operator.getType());
		}
	}
	
	private String formatAddressVictim(AddressVictim addressVictim) {
		return addressVictim.getAddress().toString();
	}
	
	private String formatAbsolute(long time) {
		ZoneId zoneId = core.getConfigs().getConfig().getObject("date-formatting.timezone", ZoneId.class);
		ZonedDateTime zonedDate = Instant.ofEpochSecond(time).atZone(zoneId);
		DateTimeFormatter formatter = core.getConfigs().getConfig().getObject("date-formatting.format", DateTimeFormatter.class);
		return formatter.format(zonedDate);
	}
	
	static {
		List<Entry<String, Long>> list = List.of(
				Map.entry("years", 31_536_000L), Map.entry("months", 2_592_000L),
				Map.entry("weeks", 604_800L), Map.entry("days", 86_400L),
				Map.entry("hours", 3_600L), Map.entry("minutes", 60L));
		@SuppressWarnings("unchecked")
		Entry<String, Long>[] asArray = (Entry<String, Long>[]) list.toArray(new Map.Entry<?, ?>[] {});
		timeUnits = asArray;
	}
	
	private String formatRelative(long diff) {
		if (diff < 0) {
			return formatRelative(-diff);
		}
		List<String> segments = new ArrayList<>();
		for (Entry<String, Long> unit : timeUnits) {
			String unitName = unit.getKey();
			long unitLength = unit.getValue();
			if (diff > unitLength && core.getConfigs().getMessages().getBoolean("misc.time." + unitName + ".enable")) {
				long amount = (diff / unitLength);
				diff -= (amount * unitLength);
				segments.add(core.getConfigs().getMessages().getString("misc.time." + unitName + ".message")
						.replace('%' + unitName.toUpperCase(Locale.ENGLISH) + '%', Long.toString(amount)));
			}
		}
		StringBuilder builder = new StringBuilder();
		final boolean comma = core.getConfigs().getMessages().getBoolean("misc.time.grammar.comma");

		for (int n = 0; n < segments.size(); n++) {
			boolean lastElement = n == segments.size() - 1;
			if (lastElement) {
				builder.append(core.getConfigs().getMessages().getString("misc.time.and"));
			} else if (comma) {
				builder.append(',');
			}
			if (n != 0) {
				builder.append(" ");
			}
			builder.append(segments.get(n));
		}
		return builder.toString();
	}
	
}
