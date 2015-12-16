package com.cie;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class NameUtils {

    private static final Logger log = Logger.getLogger(NameUtils.class.getName());

    private static final Set<String> TITLES = new HashSet<String>();
    private static final Set<String> SUFFIXES = new HashSet<String>();
    private static final Set<String> COMPOUND_NAMES = new HashSet<String>();
    public static final int TITLE = 0;
    public static final int FIRST_NAME = 1;
    public static final int MIDDLE_NAME = 2;
    public static final int LAST_NAME = 3;
    public static final int SUFFIX = 4;

    static {
        for (String title : new String[] {
            "dr.", "dr", "doctor", "mr.", "mr", "mister", "ms.", "ms", "miss", "mrs.",
            "mrs", "mistress", "hn.", "hn", "honorable", "the", "honorable", "his", "her", "honor", "fr", "fr.",
            "frau", "hr", "herr", "rv.", "rv", "rev.", "rev", "reverend", "reverend", "madam", "lord", "lady",
            "sir", "senior", "bishop", "rabbi", "holiness", "rebbe", "deacon", "eminence", "majesty", "consul",
            "vice", "president", "ambassador", "secretary", "undersecretary", "deputy", "inspector", "ins.",
            "detective", "det", "det.", "constable", "private", "pvt.", "pvt", "petty", "p.o.", "po", "first",
            "class", "p.f.c.", "pfc", "lcp.", "lcp", "corporal", "cpl.", "cpl", "colonel", "col", "col.",
            "capitain", "cpt.", "cpt", "ensign", "ens.", "ens", "lieutenant", "lt.", "lt", "ltc.", "ltc",
            "commander", "cmd.", "cmd", "cmdr", "rear", "radm", "r.adm.", "admiral", "adm.", "adm", "commodore",
            "cmd.", "cmd", "general", "gen", "gen.", "ltgen", "lt.gen.", "maj.gen.", "majgen.", "major", "maj.",
            "mjr", "maj", "seargent", "sgt.", "sgt", "chief", "cf.", "cf", "petty", "officer", "c.p.o.", "cpo",
            "master", "cmcpo", "fltmc", "formc", "mcpo", "mcpocg", "command", "fleet", "force"
        }) {
            NameUtils.TITLES.add(title);
        }

        for (String suffix : new String[] {
            "jr.", "jr", "junior", "ii", "iii", "iv", "senior", "sr.", "sr", //family
            "phd", "ph.d", "ph.d.", "m.d.", "md", "d.d.s.", "dds", // doctors
            "k.c.v.o", "kcvo", "o.o.c", "ooc", "o.o.a", "ooa", "g.b.e", "gbe", // knighthoods
            "k.b.e.", "kbe", "c.b.e.", "cbe", "o.b.e.", "obe", "m.b.e", "mbe", //   cont
            "esq.", "esq", "esquire", "j.d.", "jd", // Lawyers.
            "m.f.a.", "mfa", // Misc.
            "r.n.", "rn", "l.p.n.", "lpn", "l.n.p.", "lnp", // Nurses.
            "c.p.a.", "cpa",
            "d.d.", "dd", "d.div.", "ddiv", // Preachers.
            "ret", "ret."
        }) {
            NameUtils.SUFFIXES.add(suffix);
        }

        for (String comp : new String[] {
            "de", "la", "st", "st.", "ste", "ste.", "saint", "van", "der", "al", "bin",
            "le", "mac", "di", "del", "vel", "von", "e'", "san", "af", "el"
        }) {
            NameUtils.COMPOUND_NAMES.add(comp);
        }
    }

    /**
    * This method will parse a name into first middle and last names.
    * <p>
    * Notes: "Al" is treated as a name. "al" as a name fragment. That is the
    * only exception for capitalization.
    * </p>
    * @param name name to parse
    * @return String[5] containing title, first, middle and last names, suffix.
    */
    public static String[] parseName(String name) {
        String[] result = new String[5];
        if (name == null) {
            return result;
        }

        StringBuffer title = new StringBuffer();
        StringBuffer first = new StringBuffer();
        StringBuffer middle = new StringBuffer();
        StringBuffer last = new StringBuffer();
        StringBuffer suffix = new StringBuffer();
        boolean isLastCommaFirst = false;

        if (name.indexOf(",") != -1) {
            String[] lastRest = name.split(",");
            if (lastRest.length > 2) {
                isLastCommaFirst = true;
            }
            else {
                String[] suffixes = lastRest[1].toLowerCase().trim().split(" ");
                for (String check : suffixes) {
                    if (!NameUtils.SUFFIXES.contains(check)) {
                        isLastCommaFirst = true;
                        break;
                    }
                }
            }
        }

        // Case 1: Last Name, First Name
        if (isLastCommaFirst) {
            //log.info("Case 1: last name, first name");
            String[] lastRest = name.split(",");

            if (lastRest.length > 2) {
                for (int i = 2; i < lastRest.length; i++) {
                    // Append the remaining elements to the end of the second element.
                    lastRest[1] += (" " + lastRest[i]);
                }
            }

            result[NameUtils.LAST_NAME] = lastRest[0].trim();

            if ((lastRest.length > 1) && (lastRest[1].trim().indexOf(" ") == -1)) {
                // Easy case.
                result[NameUtils.FIRST_NAME] = lastRest[1].trim();
                return result;
            }
            else {
                String[] rest = lastRest[1].trim().split(" ");
                int head = 0;
                int tail = rest.length - 1;

                // Parse titles.
                for (int i = head; (i < rest.length) && NameUtils.TITLES.contains(rest[i].toLowerCase().trim()); i++) {
                    if (i != 0) {
                        title.append(' ');
                    }

                    title.append(rest[i]);
                    head++;
                }

                if (title.length() > 0) {
                    result[NameUtils.TITLE] = title.toString();
                }

                // Parse suffixes.
                for (int i = tail; (i >= head) && NameUtils.SUFFIXES.contains(rest[i].toLowerCase().trim()); i--) {
                    if (i != tail) {
                        suffix.insert(0, ' ');
                    }

                    suffix.insert(0, rest[i]);
                    tail--;
                }

                if (suffix.length() > 0) {
                    result[NameUtils.SUFFIX] = suffix.toString();
                }

                int[] nextNameOrder = new int[] { NameUtils.FIRST_NAME, NameUtils.MIDDLE_NAME };
                int nextNameIndex = 0;

                for (int i = head; i <= tail; i++) {
                    StringBuffer nextName = new StringBuffer();

                    while (!rest[i].trim().equals("Al") && NameUtils.COMPOUND_NAMES.contains(rest[i].toLowerCase().trim())) {
                        nextName.append(rest[i].trim());

                        if (i != tail) {
                            nextName.append(' ');
                        }

                        i++;
                        if (i == tail) {
                            break;
                        }
                    }

                    nextName.append(rest[i]);
                    result[nextNameOrder[nextNameIndex]] = nextName.toString();
                    nextNameIndex++;

                    if (nextNameIndex == nextNameOrder.length) {
                        for (int j = i + 1; j < tail; j++) {
                            if (j != (i + 1)) {
                                nextName.append(' ');
                            }
                            nextName.append(rest[j]);
                        }

                        result[nextNameOrder[nextNameIndex - 1]] = nextName.toString();
                        break;
                    }
                }
            }
        }
        else { // Case 2: First Name Last Name
            //log.info("Case 2: first name last name");
            String[] names = name.split(" ");
            int head = 0;
            int tail = names.length - 1;

            // Parse titles.
            for (int i = head; (i < tail) && NameUtils.TITLES.contains(names[i].toLowerCase().trim()); i++) {
                if (i != 0) {
                    title.append(' ');
                }

                title.append(names[i]);
                head++;
            }

            if (title.length() > 0) {
                result[NameUtils.TITLE] = title.toString();
            }

            // Parse suffixes.
            for (int i = tail; (i >= head) && NameUtils.SUFFIXES.contains(names[i].toLowerCase().trim()); i--) {
                if (i != tail) {
                    suffix.insert(0, ' ');
                }

                suffix.insert(0, names[i]);
                tail--;
            }

            if (suffix.length() > 0) {
                result[NameUtils.SUFFIX] = suffix.toString();
                names[tail] = names[tail].replaceAll(",", "");
            }

            if (head == tail) { // Only one name left.
                if (names[head].trim().length() > 0) {
                    result[NameUtils.FIRST_NAME] = names[head];
                }
            }
            else {
                // Parse last name.
                last.append(names[tail]);
                tail--;

                for (int i = tail; (i >= head) && !names[i].trim().equals("Al") &&
                     NameUtils.COMPOUND_NAMES.contains(names[i].toLowerCase().trim()); i--) {
                    last.insert(0, ' ');
                    last.insert(0, names[i]);
                    tail--;
                }

                // Parse first name.
                boolean firstPass = true;
                for (int i = head; i <= tail; i++) {
                    if (!firstPass) {
                        first.append(' ');
                    }

                    first.append(names[i].trim());
                    head++;
                    firstPass = false;

                    if (names[i].trim().equals("Al") || !NameUtils.COMPOUND_NAMES.contains(names[i].trim().toLowerCase())) {
                        break;
                    }
                }

                // Build middle name.
                for (int i = head; i <= tail; i++) {
                    if (i != head) {
                        middle.append(' ');
                    }

                    middle.append(names[i].trim());
                }
            }

            if (first.length() > 0) {
                result[NameUtils.FIRST_NAME] = first.toString().trim();
            }

            if (last.length() > 0) {
                result[NameUtils.LAST_NAME] = last.toString().trim();
            }

            if (middle.length() > 0) {
                result[NameUtils.MIDDLE_NAME] = middle.toString().trim();
            }
        }

        return result;
    }
}