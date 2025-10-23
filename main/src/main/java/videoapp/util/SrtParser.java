package videoapp.util;

/**
 * Parser for SubRip (.srt) subtitle files that produces a
 * CaptionsTrack composed of CaptionCue entries.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import videoapp.core.CaptionCue;
import videoapp.core.CaptionsTrack;

import java.io.*;
import java.util.regex.*;

public class SrtParser {
    private static final Pattern TIME = Pattern.compile(
       "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s+-->\\s+(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    );

    public static CaptionsTrack parse(File file) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            CaptionsTrack track = new CaptionsTrack();
            String line;
            StringBuilder text = null;
            long s = 0;
            long e = 0;
            boolean inBlock = false;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if(line.isEmpty()) {
                    if(inBlock && text != null) {
                        track.add(new CaptionCue(s, e, text.toString().trim()));
                    }
                    inBlock = false;
                    text = null;
                    continue;
                }
                if(!inBlock && line.matches("\\d+")) {
                    inBlock = true;
                    text = new StringBuilder();
                    String t = br.readLine();
                    if(t == null) {
                        break;
                    }
                    t = t.strip();
                    Matcher m = TIME.matcher(t);
                    if(m.find()) {
                        s = toMs(m.group(1), m.group(2), m.group(3), m.group(4));
                        e = toMs(m.group(5), m.group(6), m.group(7), m.group(8));
                    }
                } else if (inBlock) {
                    if(text.length() > 0) {
                        text.append('\n');
                    }
                    text.append(line);
                }
            }
            if(inBlock && text != null) {
                track.add(new CaptionCue(s, e, text.toString().trim()));
            }
            return track;
        }
    }
    private static long toMs(String hh, String mm, String ss, String ms) {
        int h = Integer.parseInt(hh), m = Integer.parseInt(mm), s = Integer.parseInt(ss), msI = Integer.parseInt(ms);
        return(((h * 60L + m) * 60L) + s) * 1000L + msI;
    }
}
