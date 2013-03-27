package org.mccaughey.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

public class TemporaryFileManager {
	private static Map<String, List<String>> _tempfilesInfo = new Hashtable<String, List<String>>();

	public static File getNew(HttpSession session, String prefix, String suffix)
			throws IOException {
		return getNew(session, prefix, suffix, false);
	}
	public static File getNew(HttpSession session, String prefix, String suffix,boolean doNotUseSessionId)
			throws IOException {
		synchronized (session) {
			File file = File.createTempFile(prefix + "_" + (doNotUseSessionId?"":session.getId())
					+ "_", suffix);
			file.deleteOnExit();

			List<String> infos = _tempfilesInfo.get(session.getId());
			if (infos == null)
				infos = new ArrayList<String>();
			infos.add(file.getAbsolutePath());
			_tempfilesInfo.put(session.getId(), infos);
			return file;
		}
	}

	public static void deleteAll(HttpSession session) throws IOException {
		synchronized (session) {
			List<String> infos = _tempfilesInfo.get(session.getId());
			if (infos != null) {
				for (int i = 0; i < infos.size(); i++) {
					new File(infos.get(i)).delete();
				}
			}
			_tempfilesInfo.remove(session.getId());
		}
	}
	public static void registerTempFile(HttpSession session, String absolutePath){
		synchronized (session) {
			List<String> infos = _tempfilesInfo.get(session.getId());
			if (infos == null)
				infos = new ArrayList<String>();
			infos.add(absolutePath);
			_tempfilesInfo.put(session.getId(), infos);
		}
	}

}
