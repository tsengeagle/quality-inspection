package com.tzuchi.his.quality.inspection.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ARCH-04：分層依賴方向，檢查原始碼的 import 敘述。
 *
 * <p>補 ARCH-01 看不到的情況：只引用另一層的編譯期常數時，編譯器把常數值直接寫進呼叫端，
 * 位元組碼裡沒有依賴。允許的方向與 {@link ArchitectureRules#layerDependencies(String)} 相同。
 *
 * <p>擋不到：不寫 import、直接用完整類別名稱的引用；萬用字元 import 以整個套件計。
 * 註解的去除是文字層級的：類別本體裡若有字串常值含註解符號，其後到檔尾之間不會有 import，所以不影響結果。
 */
public final class LayerImportRule {

    /** 每一層允許依賴的其他層。與 ARCH-01 的方向一致，修改時兩邊要一起改。 */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "domain", Set.of(),
            "application", Set.of("domain"),
            "reporting", Set.of("application", "domain"),
            "interfaces", Set.of("application", "reporting", "domain"),
            "infrastructure", Set.of("application", "domain"));

    private LayerImportRule() {
    }

    /** 掃描結果：掃了幾個檔案，以及違規清單（已排序，格式為「檔案路徑 imports 類別全名」）。 */
    public static final class Result {
        public final int scannedFiles;
        public final List<String> violations;

        Result(int scannedFiles, List<String> violations) {
            this.scannedFiles = scannedFiles;
            this.violations = violations;
        }
    }

    /**
     * @param sourceRoot  原始碼根目錄，例如 {@code src/main/java}
     * @param rootPackage 分層套件的上一層，例如 {@code com.tzuchi.his.quality.inspection}
     */
    public static Result scan(Path sourceRoot, String rootPackage) {
        Path base = sourceRoot.resolve(rootPackage.replace('.', '/'));
        // 在去掉註解的全文上找出每個 import 敘述（可跨行，也可以和別的敘述同一行），去掉空白後再判斷目標屬於哪一層
        Pattern importPattern = Pattern.compile("(?<![\\w.])import\\s+(?:static\\s+)?([\\w.*\\s]+?)\\s*;");
        Pattern targetPattern = Pattern.compile("^" + Pattern.quote(rootPackage) + "\\.(\\w+)\\..+$");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(base)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Set<String> violations = new TreeSet<>();
        int scanned = 0;
        for (Path file : files) {
            Path relative = base.relativize(file);
            if (relative.getNameCount() < 2) {
                continue; // 不在任何分層套件底下
            }
            String layer = relative.getName(0).toString();
            Set<String> allowed = ALLOWED.get(layer);
            if (allowed == null) {
                continue;
            }
            scanned++;
            Matcher m = importPattern.matcher(withoutComments(read(file)));
            while (m.find()) {
                String imported = m.group(1).replaceAll("\\s+", "");
                Matcher target = targetPattern.matcher(imported);
                if (!target.matches()) {
                    continue;
                }
                String targetLayer = target.group(1);
                if (ALLOWED.containsKey(targetLayer) && !targetLayer.equals(layer) && !allowed.contains(targetLayer)) {
                    violations.add(relative.toString().replace('\\', '/') + " imports " + imported);
                }
            }
        }
        return new Result(scanned, List.copyOf(violations));
    }

    /** 去掉區塊註解與行註解。import 敘述出現在類別宣告之前，不會遇到字串常值。 */
    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", "");
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
