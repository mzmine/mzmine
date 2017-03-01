/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl.helpsystem;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.Vector;

import javax.help.HelpSet;
import javax.help.Map;

public class MZmineHelpMap implements Map {

    private HelpSet helpset; // the top HelpSet
    private Hashtable<String, String> lookup = null;
    private String jarFilePath;

    public MZmineHelpMap(String jarFilePath) {
	lookup = new Hashtable<String, String>();
	this.helpset = new HelpSet();
	this.jarFilePath = jarFilePath;
    }

    public void setTarget(String target) {
	String url = "jar:file:" + jarFilePath + "!/" + target;
	lookup.put(target, url);
    }

    public void setTargetImage(String target) {
	String url = "file:" + System.getProperty("user.dir") + "/icons/"
		+ target;
	lookup.put(target, url);
    }

    /**
     * The HelpSet for this Map.
     */
    public HelpSet getHelpSet() {
	return helpset;
    }

    /**
     * Determines whether the given ID is valid. If hs is null it is ignored.
     * 
     * @param id
     *            The String ID.
     * @param hs
     *            The HelpSet against which to resolve the string.
     * @return True if id is valid, false if not valid.
     */

    public boolean isValidID(String id, HelpSet hs) {
	return lookup.containsKey(id);
    }

    /**
     * Gets an enumeration of all the IDs in a Map.
     * 
     * @return An enumeration of all the IDs in a Map.
     */
    public Enumeration<String> getAllIDs() {
	// return new FlatEnumeration(lookup.keys(), helpset);
	return lookup.keys();
    }

    /**
     * Gets the URL that corresponds to a given ID in the map.
     * 
     * @param iden
     *            The iden to get the URL for. If iden is null it is treated as
     *            an unresolved ID and will return null.
     * @return URL The matching URL. Null if this map cannot solve the ID
     * @exception MalformedURLException
     *                if the URLspecification found is malformed
     */
    public URL getURLFromID(ID iden) throws MalformedURLException {

	String id = iden.id;
	if (id == null) {
	    return null;
	}
	String tmp = null;
	try {
	    tmp = (String) lookup.get(id);
	    URL back = new URL(tmp);
	    return back;
	} catch (MissingResourceException e) {
	    return null;
	}
    }

    /**
     * Determines if the URL corresponds to an ID in the Map.
     * 
     * @param url
     *            The URL to check on.
     * @return true If this is an ID, otherwise false.
     */
    public boolean isID(URL url) {
	URL tmp;
	for (Enumeration<String> e = lookup.keys(); e.hasMoreElements();) {
	    try {
		String key = (String) e.nextElement();
		tmp = new URL((String) lookup.get(key));
		// sameFile() ignores the anchor! - epll
		if (url.sameFile(tmp) == true) {
		    return true;
		}
	    } catch (Exception ex) {
	    }
	}
	return false;
    }

    /**
     * Gets the ID for this URL.
     * 
     * @param url
     *            The URL to get the ID for.
     * @return The id (Map.ID) or null if URL is not an ID.
     */
    public ID getIDFromURL(URL url) {
	String tmp;
	URL tmpURL;
	if (url == null)
	    return null;
	String urlString = url.toExternalForm();
	for (Enumeration<String> e = lookup.keys(); e.hasMoreElements();) {
	    String key = (String) e.nextElement();
	    try {
		tmp = (String) lookup.get(key);

		// Sometimes tmp will be null because not all keys are ids
		if (tmp == null)
		    continue;

		tmpURL = new URL(tmp);
		String tmpString = tmpURL.toExternalForm();
		if (urlString.compareTo(tmpString) == 0) {
		    return ID.create(key, helpset);
		}
	    } catch (Exception ex) {
	    }
	}
	return null;
    }

    /**
     * Determines the ID that is "closest" to this URL (with a given anchor).
     * 
     * The definition of this is up to the implementation of Map. In particular,
     * it may be the same as getIDFromURL().
     * 
     * @param url
     *            A URL
     * @return The closest ID in this map to the given URL
     */
    public ID getClosestID(URL url) {
	return getIDFromURL(url);
    }

    /**
     * Determines the IDs related to this URL.
     * 
     * @param URL
     *            The URL to compare the Map IDs to.
     * @return Enumeration of Map.IDs
     */
    public Enumeration<Object> getIDs(URL url) {
	String tmp = null;
	URL tmpURL = null;
	Vector<String> ids = new Vector<String>();
	for (Enumeration<String> e = lookup.keys(); e.hasMoreElements();) {
	    String key = (String) e.nextElement();
	    try {
		tmp = (String) lookup.get(key);
		tmpURL = new URL(tmp);
		if (url.sameFile(tmpURL) == true) {
		    ids.addElement(key);
		}
	    } catch (Exception ex) {
	    }
	}
	return new FlatEnumeration(ids.elements(), helpset);
    }

    private static class FlatEnumeration implements Enumeration<Object> {
	private Enumeration<String> e;
	private HelpSet hs;

	public FlatEnumeration(Enumeration<String> e, HelpSet hs) {
	    this.e = e;
	    this.hs = hs;
	}

	public boolean hasMoreElements() {
	    return e.hasMoreElements();
	}

	public Object nextElement() {
	    Object back = null;
	    try {
		back = ID.create((String) e.nextElement(), hs);
	    } catch (Exception ex) {
	    }
	    return back;
	}
    }

}
