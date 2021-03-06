/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aurora.scheduler.http.api.security;

import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.twitter.common.args.ArgParser;
import com.twitter.common.args.parsers.NonParameterizedTypeParser;

import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.config.Ini;
import org.apache.shiro.realm.text.IniRealm;

/**
 * Parser for shiro.ini files. Accepts any string that {@link Ini#fromResourcePath(String)} does.
 * The provided ini file must have only the sections required for configuration
 * ({@link IniRealm.ROLES_SECTION_NAME} and {@link IniRealm.USERS_SECTION_NAME}) and no extras -
 * Aurora uses Guice in to configure those sections in
 * {@link HttpSecurityModule}}.
 */
@ArgParser
public class ShiroIniParser extends NonParameterizedTypeParser<Ini> {
  @VisibleForTesting
  static final Set<String> REQUIRED_SECTION_NAMES =
      ImmutableSortedSet.of(IniRealm.ROLES_SECTION_NAME, IniRealm.USERS_SECTION_NAME);

  @VisibleForTesting
  static class MissingSectionsException extends IllegalArgumentException {
    MissingSectionsException(Set<String> missingSections) {
      super("Missing required sections: " + missingSections);
    }
  }

  @VisibleForTesting
  static class ExtraSectionsException extends IllegalArgumentException {
    ExtraSectionsException(Set<String> extraSections) {
      super("Extra sections present: " + extraSections);
    }
  }

  @VisibleForTesting
  static class ShiroConfigurationException extends IllegalArgumentException {
    ShiroConfigurationException(ConfigurationException e) {
      super(e);
    }
  }

  @Override
  public Ini doParse(String raw) throws IllegalArgumentException {
    Ini ini;
    try {
      ini = Ini.fromResourcePath(raw);
    } catch (ConfigurationException e) {
      throw new ShiroConfigurationException(e);
    }

    Set<String> presentSections = ImmutableSortedSet.copyOf(ini.getSectionNames());
    Set<String> missingSections = Sets.difference(REQUIRED_SECTION_NAMES, presentSections);
    if (!missingSections.isEmpty()) {
      throw new MissingSectionsException(missingSections);
    }

    Set<String> extraSections = Sets.difference(presentSections, REQUIRED_SECTION_NAMES);
    if (!extraSections.isEmpty()) {
      throw new ExtraSectionsException(extraSections);
    }

    return ini;
  }
}
