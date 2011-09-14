package activityclassifier.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extractor {

    public final Pattern pattern;

    public Extractor(Pattern pattern) {
        this.pattern = pattern;
    }

    public String extractSingle(String line)
    {
        if (!line.isEmpty()) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                int groups = matcher.groupCount();
                if (groups==0) {
                    String value = matcher.group();
                    if (value!=null && !value.isEmpty())
                        return value;
                } else {
                    for (int i=1; i<=groups; ++i) {
                        String value = matcher.group(i);
                        if (value!=null && !value.isEmpty())
                            return value;
                    }
                }
            }
        }
        return "";
    }

    public String[] extractMany(String line, List<String> output)
    {
        output.clear();
        if (!line.isEmpty()) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                int groups = matcher.groupCount();
                if (groups==0) {
                    String value = matcher.group();
                    if (value!=null && !value.isEmpty())
                        output.add(value);
                } else {
                    for (int i=1; i<=groups; ++i) {
                        String value = matcher.group(i);
                        if (value!=null && !value.isEmpty())
                            output.add(value);
                    }
                }
            }
        }
        return output.toArray(new String[output.size()]);
    }

}
