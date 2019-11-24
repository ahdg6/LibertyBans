/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.backend.subjects;

import java.util.UUID;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InvalidUUIDException;
import space.arim.bans.api.util.MiscUtil;

public class Subjects implements SubjectsMaster {
	
	private final ArimBans center;
	
	private static final int LENGTH_OF_FULL_UUID = 36;
	private static final int LENGTH_OF_SHORT_UUID = 32;

	public Subjects(ArimBans center) {
		this.center = center;
		refreshConfig();
	}
	
	@Override
	public boolean isOnline(Subject subject) {
		return center.environment().isOnline(subject);
	}
	
	@Override
	public Subject parseSubject(String input, boolean consolable) throws IllegalArgumentException {
		if (center.checkAddress(input)) {
			return Subject.fromIP(input);
		} else if (input.length() == LENGTH_OF_FULL_UUID) {
			try {
				return parseSubject(UUID.fromString(input));
			} catch (IllegalArgumentException ex) {
				throw new InvalidUUIDException("UUID " + input + " does not conform.");
			}
		} else if (input.length() == LENGTH_OF_SHORT_UUID) {
			return parseSubject(MiscUtil.expandUUID(input));
		} else if (consolable && input.equalsIgnoreCase(center.formats().getConsoleDisplay())) {
			return Subject.console();
		}
		throw new IllegalArgumentException("Could not make " + input + " into a subject");
	}
	
	@Override
	public Subject parseSubject(UUID input) {
		if (checkUUID(input)) {
			return Subject.fromUUID(input);
		}
		throw new InvalidUUIDException(input);
	}
	
	@Override
	public boolean checkUUID(UUID uuid) {
		return center.cache().uuidExists(uuid);
	}
}
