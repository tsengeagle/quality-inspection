package com.tzuchi.his.quality.inspection.acceptance.http;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 契約裡的回應形狀與它的比對。
 *
 * <ul>
 *   <li>對應表：封閉的物件。列出的欄位都要有，沒列出的欄位不可以出現。</li>
 *   <li>只有一個元素的清單：陣列，元素都要符合那個形狀。</li>
 *   <li>字串：純量型別 {@code id}、{@code text}、{@code int}、{@code decimal}、{@code boolean}、
 *       {@code enum <名稱>}，結尾加 {@code ?} 表示可以是 null；字面值 {@code null}、{@code true}、{@code false}；
 *       {@code $data} 只能出現在共用層的外殼，代表端點的 data 形狀。</li>
 * </ul>
 *
 * <p>形狀無效時 {@link #validate} 記為載入問題；{@link #check} 不丟例外，只把該位置記為無法比對，
 * 報告才能照常寫出本次的結果。</p>
 */
final class JsonShape {

    static final String DATA = "$data";
    private static final Set<String> SCALARS = Set.of("id", "text", "int", "decimal", "boolean");
    private static final Set<String> LITERALS = Set.of("null", "true", "false");
    private static final Pattern ENUM = Pattern.compile("^enum (\\w+)$");

    private JsonShape() {}

    /** 載入時檢查形狀本身，問題加進 problems；陣列的位置（索引寫成 []）加進 arrays。 */
    static void validate(Object shape, String path, Map<String, Set<String>> enums, boolean allowData,
                         List<String> problems, List<String> arrays) {
        if (shape instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) shape).entrySet()) {
                validate(e.getValue(), path + "." + e.getKey(), enums, allowData, problems, arrays);
            }
        } else if (shape instanceof List) {
            List<?> list = (List<?>) shape;
            if (list.size() != 1) {
                problems.add(path + "：陣列的形狀必須剛好一個元素，實際 " + list.size() + " 個");
                return;
            }
            arrays.add(path);
            validate(list.get(0), path + "[]", enums, allowData, problems, arrays);
        } else if (shape instanceof String) {
            String s = (String) shape;
            if (DATA.equals(s)) {
                if (!allowData) {
                    problems.add(path + "：$data 只能用在共用層的外殼");
                }
                return;
            }
            if (LITERALS.contains(s)) {
                return;
            }
            String base = s.endsWith("?") ? s.substring(0, s.length() - 1) : s;
            Matcher m = ENUM.matcher(base);
            if (m.matches()) {
                if (!enums.containsKey(m.group(1))) {
                    problems.add(path + "：列舉 " + m.group(1) + " 沒有定義（找不到 model.yaml 的 enums）");
                }
            } else if (!SCALARS.contains(base)) {
                problems.add(path + "：不認得的型別 " + s);
            }
        } else {
            problems.add(path + "：形狀必須是對應表、清單或字串，實際是 " + shape
                    + "（字面值 true、false、null 要加引號）");
        }
    }

    /** 比對實際的 JSON。arraysSeen 收集非空陣列的位置（索引寫成 []）。 */
    static void check(Object shape, JsonNode node, String path, Object dataShape, Map<String, Set<String>> enums,
                      List<String> out, Set<String> arraysSeen) {
        check(shape, node, path, path, dataShape, enums, out, arraysSeen);
    }

    private static void check(Object shape, JsonNode node, String path, String norm, Object dataShape,
                              Map<String, Set<String>> enums, List<String> out, Set<String> arraysSeen) {
        if (shape == null) {
            out.add(path + "：契約沒有這個位置的形狀，無法比對");
            return;
        }
        if (DATA.equals(shape)) {
            check(dataShape, node, path, norm, null, enums, out, arraysSeen);
            return;
        }
        if (shape instanceof Map) {
            if (node == null || node.isNull()) {
                out.add(path + "：不可為 null");
                return;
            }
            if (!node.isObject()) {
                out.add(path + "：預期物件，實際是" + kind(node));
                return;
            }
            Map<?, ?> fields = (Map<?, ?>) shape;
            for (Map.Entry<?, ?> e : fields.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (!node.has(key)) {
                    out.add(path + "：缺少欄位 " + key);
                } else {
                    check(e.getValue(), node.get(key), path + "." + key, norm + "." + key, dataShape, enums, out, arraysSeen);
                }
            }
            for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                if (!fields.containsKey(key)) {
                    out.add(path + "：契約沒有的欄位 " + key);
                }
            }
            return;
        }
        if (shape instanceof List) {
            if (node == null || node.isNull()) {
                out.add(path + "：不可為 null");
                return;
            }
            if (!node.isArray()) {
                out.add(path + "：預期陣列，實際是" + kind(node));
                return;
            }
            if (((List<?>) shape).size() != 1) {
                out.add(path + "：契約的陣列形狀無效（見載入問題），無法比對");
                return;
            }
            if (node.size() > 0) {
                arraysSeen.add(norm);
            }
            Object element = ((List<?>) shape).get(0);
            for (int i = 0; i < node.size(); i++) {
                check(element, node.get(i), path + "[" + i + "]", norm + "[]", dataShape, enums, out, arraysSeen);
            }
            return;
        }
        if (!(shape instanceof String)) {
            out.add(path + "：契約的形狀無效（見載入問題），無法比對");
            return;
        }
        String s = (String) shape;
        switch (s) {
            case "null":
                if (!node.isNull()) out.add(path + "：預期 null，實際是" + kind(node));
                return;
            case "true":
            case "false":
                if (!node.isBoolean() || node.booleanValue() != Boolean.parseBoolean(s)) {
                    out.add(path + "：預期 " + s + "，實際是 " + node);
                }
                return;
            default:
        }
        boolean nullable = s.endsWith("?");
        String base = nullable ? s.substring(0, s.length() - 1) : s;
        if (node.isNull()) {
            if (!nullable) out.add(path + "：不可為 null");
            return;
        }
        Matcher m = ENUM.matcher(base);
        if (m.matches()) {
            if (!node.isTextual()) {
                out.add(path + "：預期字串，實際是" + kind(node));
            } else if (!enums.getOrDefault(m.group(1), Set.of()).contains(node.textValue())) {
                out.add(path + "：" + node.textValue() + " 不是 " + m.group(1) + " 的值");
            }
            return;
        }
        switch (base) {
            case "id":
            case "text":
                if (!node.isTextual()) out.add(path + "：預期字串，實際是" + kind(node));
                return;
            case "int":
                if (!node.isIntegralNumber()) out.add(path + "：預期整數，實際是 " + node);
                return;
            case "decimal":
                if (!node.isNumber()) out.add(path + "：預期數字，實際是" + kind(node));
                return;
            case "boolean":
                if (!node.isBoolean()) out.add(path + "：預期布林，實際是" + kind(node));
                return;
            default:
                out.add(path + "：不認得的型別 " + s);
        }
    }

    static String kind(JsonNode node) {
        if (node == null || node.isMissingNode()) return "缺少";
        if (node.isNull()) return " null";
        if (node.isArray()) return "陣列";
        if (node.isObject()) return "物件";
        if (node.isTextual()) return "字串";
        if (node.isNumber()) return "數字";
        if (node.isBoolean()) return "布林";
        return node.getNodeType().toString();
    }
}
