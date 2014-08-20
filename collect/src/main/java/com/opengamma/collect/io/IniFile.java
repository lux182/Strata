/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.opengamma.collect.ArgChecker;

/**
 * An INI file.
 * <p>
 * Represents an INI file together with the ability to parse it from a {@link CharSource}.
 * <p>
 * The INI file format used here is deliberately simple.
 * There are two elements - key-value pairs and sections.
 * <p>
 * The basic element is a key-value pair.
 * The key is separated from the value using the '=' symbol.
 * Duplicate keys are allowed.
 * For example 'key = value'.
 * <p>
 * All properties are grouped into named sections.
 * The section name occurs on a line by itself surrounded by square brackets.
 * Duplicate section names are not allowed.
 * For example '[section]'.
 * <p>
 * Keys, values and section names are trimmed.
 * Blank lines are ignored.
 * Whole line comments begin with hash '#' or semicolon ';'.
 * No escape format is available.
 * Lookup is case sensitive.
 * <p>
 * This example explains the format:
 * <pre>
 *  # line comment
 *  [foo]
 *  key = value
 * 
 *  [bar]
 *  key = value
 *  month = January
 * </pre>
 * <p>
 * The aim of this class is to parse the basic format.
 * Interpolation of variables is not supported.
 */
public final class IniFile {

  /**
   * The INI sections.
   */
  private final ImmutableMap<String, PropertySet> sectionMap;

  //-------------------------------------------------------------------------
  /**
   * Parses the specified source as an INI file.
   * <p>
   * This parses the specified character source expecting an INI file format.
   * The resulting instance can be queried for each section in the file.
   * 
   * @param source  the INI file resource, not null
   * @return the INI file, not null
   */
  public static IniFile of(CharSource source) {
    ArgChecker.notNull(source, "source");
    try {
      Map<String, Multimap<String, String>> parsedIni = parse(source);
      ImmutableMap.Builder<String, PropertySet> builder = ImmutableMap.builder();
      parsedIni.forEach((sectionName, sectionData) -> builder.put(sectionName, PropertySet.of(sectionData)));
      return new IniFile(builder.build());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // parses the INI file format
  private static Map<String, Multimap<String, String>> parse(CharSource source) throws IOException {
    ImmutableList<String> lines = source.readLines();
    Map<String, Multimap<String, String>> ini = new LinkedHashMap<>();
    Multimap<String, String> currentSection = null;
    int lineNum = 0;
    for (String line : lines) {
      lineNum++;
      line = line.trim();
      if (line.length() == 0 || line.startsWith("#") || line.startsWith(";")) {
        continue;
      }
      if (line.startsWith("[") && line.endsWith("]")) {
        String sectionName = line.substring(1, line.length() - 1).trim();
        if (ini.containsKey(sectionName)) {
          throw new IllegalArgumentException("Invalid INI file, duplicate section no allowed, line " + lineNum);
        }
        currentSection = ArrayListMultimap.create();
        ini.put(sectionName, currentSection);
        
      } else if (currentSection == null) {
        throw new IllegalArgumentException("Invalid INI file, properties must be within a [section], line " + lineNum);
        
      } else {
        int equalsPosition = line.indexOf('=');
        if (equalsPosition < 0) {
          throw new IllegalArgumentException("Invalid INI file, expected key=value property, line " + lineNum);
        }
        String key = line.substring(0, equalsPosition).trim();
        String value = line.substring(equalsPosition + 1).trim();
        if (key.length() == 0) {
          throw new IllegalArgumentException("Invalid INI file, empty key, line " + lineNum);
        }
        currentSection.put(key, value);
      }
    }
    return ini;
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param sectionMap  the sections
   */
  private IniFile(ImmutableMap<String, PropertySet> sectionMap) {
    this.sectionMap = sectionMap;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this INI file contains the specified section.
   * 
   * @param name  the section name
   * @return true if the section exists
   */
  public boolean contains(String name) {
    ArgChecker.notNull(name, "name");
    return sectionMap.containsKey(name);
  }

  /**
   * Gets a single section of this INI file.
   * <p>
   * This returns the section associated with the specified name.
   * If the section does not exist an exception is thrown.
   * 
   * @param name  the section name
   * @return the INI file section
   * @throws IllegalArgumentException if the section does not exist
   */
  public PropertySet getSection(String name) {
    ArgChecker.notNull(name, "name");
    if (contains(name) == false) {
      throw new IllegalArgumentException("Unknown INI file section: " + name);
    }
    return sectionMap.get(name);
  }

  /**
   * Gets all the sections of this INI file.
   * <p>
   * The iteration order of the map matches that of the original file.
   * 
   * @return the INI file sections
   */
  public ImmutableMap<String, PropertySet> getSections() {
    return sectionMap;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this INI file equals another.
   * <p>
   * The comparison checks the content.
   * 
   * @param obj  the other file, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof IniFile) {
      return sectionMap.equals(((IniFile) obj).sectionMap);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the INI file.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return sectionMap.hashCode();
  }

  /**
   * Returns a string describing the INI file.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return sectionMap.toString();
  }

}