package com.tzuchi.his.quality.inspection.acceptance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * spec_version 2 的路徑（D-065 第 3 點）：{@code .}、{@code a.b}、{@code a[*]}、{@code a[k=v]}、{@code a[0]}。
 *
 * <p>只負責「選到哪些值」；選到之後怎麼判定（output、absent、only、save）由 {@link ExampleRunner} 決定。</p>
 */
final class TreePath {

    /** 路徑走到一個不存在的屬性。與「屬性存在但值是 null」分開，讓呼叫端決定兩者是否等價。 */
    static final Object MISSING = new Object() {
        @Override
        public String toString() { return "（不存在）"; }
    };

    private static final Pattern SEGMENT = Pattern.compile("^([A-Za-z0-9_]+)(?:\\[([^\\]]*)])?$");
    private static final Pattern INDEX = Pattern.compile("^[0-9]+$");

    private enum Kind { PLAIN, ALL, FILTER, INDEX }

    private static final class Segment {
        final String name;
        final Kind kind;
        final Map<String, String> conditions;
        final int index;

        Segment(String name, Kind kind, Map<String, String> conditions, int index) {
            this.name = name;
            this.kind = kind;
            this.conditions = conditions;
            this.index = index;
        }
    }

    private final String text;
    private final List<Segment> segments;

    private TreePath(String text, List<Segment> segments) {
        this.text = text;
        this.segments = segments;
    }

    /** @throws RunnerProblem 路徑的寫法不對 */
    static TreePath parse(String text) {
        List<Segment> segments = new ArrayList<>();
        if (".".equals(text)) {
            return new TreePath(text, segments);
        }
        for (String part : split(text)) {
            Matcher m = SEGMENT.matcher(part);
            if (!m.matches()) {
                throw new RunnerProblem("路徑的寫法不對：" + text);
            }
            String name = m.group(1);
            String bracket = m.group(2);
            if (bracket == null) {
                segments.add(new Segment(name, Kind.PLAIN, null, -1));
            } else if ("*".equals(bracket)) {
                segments.add(new Segment(name, Kind.ALL, null, -1));
            } else if (INDEX.matcher(bracket).matches()) {
                segments.add(new Segment(name, Kind.INDEX, null, Integer.parseInt(bracket)));
            } else {
                Map<String, String> conditions = new LinkedHashMap<>();
                for (String condition : bracket.split(",")) {
                    int eq = condition.indexOf('=');
                    if (eq <= 0) {
                        throw new RunnerProblem("路徑的條件必須是 k=v：" + text);
                    }
                    conditions.put(condition.substring(0, eq).trim(), condition.substring(eq + 1).trim());
                }
                segments.add(new Segment(name, Kind.FILTER, conditions, -1));
            }
        }
        return new TreePath(text, segments);
    }

    /** 一個字串是不是需要當成路徑解讀（含 . 或 [ ]）；否則是單純的屬性名稱。 */
    static boolean isPath(String key) {
        return key.indexOf('.') >= 0 || key.indexOf('[') >= 0 || key.indexOf(']') >= 0;
    }

    /** 有沒有 [*] 或 [k=v]：save 不能用。 */
    boolean selectsMany() {
        for (Segment s : segments) {
            if (s.kind == Kind.ALL || s.kind == Kind.FILTER) {
                return true;
            }
        }
        return false;
    }

    /** 有沒有 [0]：只有 save 能用。 */
    boolean usesIndex() {
        for (Segment s : segments) {
            if (s.kind == Kind.INDEX) {
                return true;
            }
        }
        return false;
    }

    /**
     * 從 root 選出路徑指到的值。
     *
     * <ul>
     *   <li>屬性不存在（或上一層是 null、不是物件）時，結果是 {@link #MISSING}。</li>
     *   <li>{@code [*]}、{@code [k=v]}、{@code [0]} 指到的屬性不存在或為 null 時，選不到任何一筆。</li>
     *   <li>它們指到的屬性存在卻不是清單，是範例與程式對不上：丟 AssertionError。</li>
     * </ul>
     *
     * @param equal 條件 {@code k=v} 的比較：(規格寫的值, 實際的值)
     */
    List<Object> select(Object root, BiPredicate<Object, Object> equal) {
        List<Object> current = new ArrayList<>();
        current.add(root);
        for (Segment segment : segments) {
            List<Object> next = new ArrayList<>();
            for (Object node : current) {
                Object value = property(node, segment.name);
                if (segment.kind == Kind.PLAIN) {
                    next.add(value);
                    continue;
                }
                if (value == MISSING || value == null) {
                    continue;
                }
                if (!(value instanceof Collection)) {
                    throw new AssertionError("路徑 " + text + "：" + segment.name + " 不是清單，實際是 " + value);
                }
                List<Object> items = new ArrayList<>((Collection<?>) value);
                if (segment.kind == Kind.ALL) {
                    next.addAll(items);
                } else if (segment.kind == Kind.INDEX) {
                    if (segment.index < items.size()) {
                        next.add(items.get(segment.index));
                    }
                } else {
                    for (Object item : items) {
                        if (matches(item, segment.conditions, equal)) {
                            next.add(item);
                        }
                    }
                }
            }
            current = next;
        }
        return current;
    }

    private static boolean matches(Object item, Map<String, String> conditions, BiPredicate<Object, Object> equal) {
        if (!(item instanceof Map)) {
            return false;
        }
        Map<?, ?> map = (Map<?, ?>) item;
        for (Map.Entry<String, String> condition : conditions.entrySet()) {
            if (!map.containsKey(condition.getKey()) || !equal.test(condition.getValue(), map.get(condition.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static Object property(Object node, String name) {
        if (!(node instanceof Map)) {
            return MISSING;
        }
        Map<?, ?> map = (Map<?, ?>) node;
        return map.containsKey(name) ? map.get(name) : MISSING;
    }

    /** 依 . 切開，中括號裡的 . 不算（條件的值可以含 .）。 */
    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        int depth = 0;
        for (char c : text.toCharArray()) {
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            }
            if (c == '.' && depth == 0) {
                parts.add(part.toString());
                part.setLength(0);
            } else {
                part.append(c);
            }
        }
        parts.add(part.toString());
        return parts;
    }

    @Override
    public String toString() {
        return text;
    }
}
