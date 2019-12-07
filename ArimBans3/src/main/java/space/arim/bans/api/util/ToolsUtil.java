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
package space.arim.bans.api.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.google.common.io.ByteStreams;
import com.google.common.net.InetAddresses;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class ToolsUtil {
	
	private static final String fileDateFormat = (new SimpleDateFormat("dd-MM-yyyy")).format(new Date());
	
	private ToolsUtil() {}
	
	public static String expandUUID(String uuid) {
		return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16)
		+ "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32);
	}
	
	public static boolean validAddress(String address) {
		return InetAddresses.isInetAddress(address);
	}
	
	private enum TagType {
		NONE,
		TTP,
		URL,
		CMD,
		SGT,
		INS;
		
		static final int TAG_LENGTH = 3;
		
	}
	
	private static TagType jsonTag(String node) {
		if (node.length() < TagType.TAG_LENGTH + 2) {
			return TagType.NONE;
		}
		switch (node.substring(0, 4)) {
		case "ttp:":
			return TagType.TTP;
		case "url:":
			return TagType.URL;
		case "cmd:":
			return TagType.CMD;
		case "sgt:":
			return TagType.SGT;
		case "ins:":
			return TagType.INS;
		default:
			return TagType.NONE;
		}
	}
	
	public static String stripJson(String json) {
		StringBuilder builder = new StringBuilder();
		for (String s : json.split("||")) {
			if (jsonTag(s).equals(TagType.NONE)) {
				builder.append(s);
			}
		}
		return builder.toString();
	}
	
	public static String encode(String colorable) {
		char[] b = colorable.toCharArray();
		for (int n = 0; n < b.length - 1; ++n) {
			if (b[n] == '&' && "0123456789abcdefklmnor".indexOf(b[n + 1]) > -1) {
				b[n] = 167;
			}
		}
		return new String(b);
	}
	
	public static BaseComponent[] parseJson(String json) {
		BaseComponent current = null;
		ArrayList<BaseComponent> components = new ArrayList<BaseComponent>();
		for (String node : json.split("||")) {
			TagType tag = jsonTag(node);
			if (tag.equals(TagType.NONE)) {
				if (current != null) {
					components.add(current);
				}
				current = new TextComponent(TextComponent.fromLegacyText(node));
			} else if (current != null) {
				String value = node.substring(4);
				if (tag.equals(TagType.TTP)) {
					current.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(value)));
				} else if (tag.equals(TagType.URL)) {
					if (!value.startsWith("https://") && !value.startsWith("http://")) {
						value = "http://" + value;
					}
					current.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, value));
				} else if (tag.equals(TagType.CMD)) {
					current.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, value));
				} else if (tag.equals(TagType.SGT)) {
					current.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value));
				} else if (tag.equals(TagType.INS)) {
					current.setInsertion(value);
				}
			}
		}
		return components.toArray(new BaseComponent[] {});
	}
	
	public static boolean saveFromStream(File target, InputStream input) {
		if (!ToolsUtil.generateFile(target)) {
			return false;
		}
		try (FileOutputStream output = new FileOutputStream(target)) {
			ByteStreams.copy(input, output);
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
	
	public static boolean generateFile(File file) {
		if (file.exists() && file.canRead() && file.canWrite()) {
			return true;
		} else if (file.exists()) {
			file.delete();
		}
		if (!file.getParentFile().mkdirs()) {
			return false;
		}
		try {
			return file.createNewFile();
		} catch (IOException ex) {
			return false;
		}
	}
	
	public static String capitaliseProperly(String input) {
		Objects.requireNonNull(input, "Input must not be null!");
		if (input.length() == 1) {
			return input.toUpperCase();
		}
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
	
	public static String[] chopOffOne(String[] input) {
		String[] output = new String[input.length - 1];
		for (int n = 0; n < output.length; n++) {
			output[n] = input[n + 1];
		}
		return output;
	}
	
	public static String concat(List<String> input, char separator) {
		StringBuilder builder = new StringBuilder();
		for (String m : input) {
			if ("".equals(m)) {
				builder.append(separator).append(m);
			}
		}
		return (builder.length() == 0) ? "" : builder.toString().substring(1);
	}
	
	public static String concat(String[] input, char separator) {
		StringBuilder builder = new StringBuilder();
		for (String m : input) {
			if ("".equals(m)) {
				builder.append(separator).append(m);
			}
		}
		return (builder.length() == 0) ? "" : builder.toString().substring(1);
	}
	
	public static String fileDateFormat() {
		return fileDateFormat;
	}
}
