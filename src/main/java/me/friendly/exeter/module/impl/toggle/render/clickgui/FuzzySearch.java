package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class FuzzySearch {

  private static final int EXACT_TIER = 100000;
  private static final int TYPO_TIER = 50000;

  private FuzzySearch() {}

  public static int score(String pattern, String text) {
    String q = pattern.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    if (q.isEmpty()) return 0;
    String t = text.toLowerCase(Locale.ROOT);

    double best = orderedScore(q, t, text);
    if (best != Double.NEGATIVE_INFINITY) {
      return EXACT_TIER + Math.max(0, (int) Math.round(best * 100));
    }

    // nothing matched in order so js try le typos
    String bare = t.replace(" ", "");
    int distance = typoDistance(q, bare);
    if (distance < 0) return -1;
    return Math.max(1, TYPO_TIER - distance * 1000 - Math.abs(bare.length() - q.length()) * 10);
  }

  public static <T> List<T> filter(List<T> items, String pattern, Function<T, String> labelGetter) {
    if (pattern.replaceAll("\\s+", "").isEmpty()) {
      return new ArrayList<>(items);
    }

    List<Scored<T>> scored = new ArrayList<>();
    for (T item : items) {
      String label = labelGetter.apply(item);
      int s = score(pattern, label);
      if (s >= 0) {
        scored.add(new Scored<>(item, label, s));
      }
    }

    scored.sort(
        (a, b) -> {
          int byScore = Integer.compare(b.score, a.score);
          // if same score, go inn le alphabetical whatever
          return byScore != 0 ? byScore : a.label.compareToIgnoreCase(b.label);
        });

    List<T> result = new ArrayList<>(scored.size());
    for (Scored<T> s : scored) {
      result.add(s.item);
    }
    return result;
  }

  private static double orderedScore(String q, String t, String original) {
    double best = Double.NEGATIVE_INFINITY;

    // ig js try every spot the first letter shows up
    for (int start = 0; start < t.length(); start++) {
      if (t.charAt(start) != q.charAt(0)) continue;

      int qi = 0;
      int prev = -2;
      int first = -1;
      int last = -1;
      double score = 0;

      for (int ti = start; ti < t.length() && qi < q.length(); ti++) {
        if (t.charAt(ti) != q.charAt(qi)) continue;

        double points = 1;
        if (ti == prev + 1) points += 3;
        if (isBoundary(original, ti)) points += 4;

        score += points;
        if (first < 0) first = ti;
        last = ti;
        prev = ti;
        qi++;
      }

      if (qi < q.length()) continue;

      // spread out matches and long names lose points
      score -= (last - first - (q.length() - 1)) * 0.6;
      score -= (t.length() - q.length()) * 0.05;
      if (t.contains(q)) score += 8;
      if (t.startsWith(q)) score += 6;

      if (score > best) best = score;
    }

    return best;
  }

  private static int typoDistance(String q, String t) {
    int m = q.length();
    int limit = m <= 3 ? 0 : m <= 6 ? 1 : 2;
    if (limit == 0) return -1;

    int n = t.length();
    int[] prev2 = null;
    int[] prev = new int[n + 1];

    for (int i = 1; i <= m; i++) {
      int[] cur = new int[n + 1];
      cur[0] = i;
      for (int j = 1; j <= n; j++) {
        int cost = q.charAt(i - 1) == t.charAt(j - 1) ? 0 : 1;
        int v = Math.min(Math.min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost);
        // swapped neighbours so it only count once
        if (i > 1
            && j > 1
            && q.charAt(i - 1) == t.charAt(j - 2)
            && q.charAt(i - 2) == t.charAt(j - 1)) {
          v = Math.min(v, prev2[j - 2] + 1);
        }
        cur[j] = v;
      }
      prev2 = prev;
      prev = cur;
    }

    int d = Arrays.stream(prev).min().getAsInt();
    return d <= limit ? d : -1;
  }

  private static boolean isBoundary(String text, int i) {
    if (i == 0) return true;
    char prev = text.charAt(i - 1);
    if (prev == ' ' || prev == '_' || prev == '-') return true;
    return Character.isLowerCase(prev) && Character.isUpperCase(text.charAt(i));
  }

  private static final class Scored<T> {
    final T item;
    final String label;
    final int score;

    Scored(T item, String label, int score) {
      this.item = item;
      this.label = label;
      this.score = score;
    }
  }
}
