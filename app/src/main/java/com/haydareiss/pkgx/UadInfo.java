package com.haydareiss.pkgx;

import java.util.Collections;
import java.util.List;

/** Removal advice for one package, taken from the Universal Android Debloater list. */
final class UadInfo {

    enum Level {
        RECOMMENDED, ADVANCED, EXPERT, UNSAFE, UNLISTED;

        static Level parse(String value) {
            if (value == null) return UNLISTED;
            switch (value) {
                case "Recommended":
                    return RECOMMENDED;
                case "Advanced":
                    return ADVANCED;
                case "Expert":
                    return EXPERT;
                case "Unsafe":
                    return UNSAFE;
                default:
                    return UNLISTED;
            }
        }
    }

    static final UadInfo UNLISTED = new UadInfo(
            Level.UNLISTED, "", "", Collections.emptyList(), Collections.emptyList());

    final Level level;
    /** Source list: Oem, Aosp, Google, Carrier or Misc. */
    final String list;
    final String description;
    final List<String> dependencies;
    final List<String> neededBy;

    UadInfo(Level level, String list, String description,
            List<String> dependencies, List<String> neededBy) {
        this.level = level;
        this.list = list;
        this.description = description;
        this.dependencies = dependencies;
        this.neededBy = neededBy;
    }

    boolean listed() {
        return level != Level.UNLISTED;
    }
}
