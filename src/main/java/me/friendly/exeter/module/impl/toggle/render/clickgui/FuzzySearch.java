package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class FuzzySearch {

  private FuzzySearch() {}

  public static int score(String pattern, String text) {
    if (pattern.isEmpty()) return 0;
    String pl = pattern.toLowerCase();
    String tl = text.toLowerCase();

    int pi = 0;
    int prevMatch = -1;
    int score = 0;
    int consecutive = 0;

    for (int ti = 0; ti < tl.length() && pi < pl.length(); ti++) {
      if (tl.charAt(ti) == pl.charAt(pi)) {
        int gap = prevMatch >= 0 ? ti - prevMatch : 5;
        prevMatch = ti;

        if (gap <= 1) {
          consecutive++;
          score += consecutive * 10;
        } else {
          consecutive = 0;
          score += 1;
        }

        if (ti == 0) {
          score += 15;
        } else {
          char prev = text.charAt(ti - 1);
          if (prev == ' ' || prev == '_' || prev == '-') {
            score += 12;
          } else if (Character.isUpperCase(prev) && Character.isLowerCase(text.charAt(ti))) {
            score += 8;
          }
        }

        if (ti == pi) {
          score += 5;
        }

        pi++;
      }
    }

    if (pi < pl.length()) return -1;

    score += Math.max(0, 20 - text.length());

    return score;
  }

  public static <T> List<T> filter(List<T> items, String pattern, Function<T, String> labelGetter) {
    if (pattern.isEmpty()) {
      return new ArrayList<>(items);
    }
    List<Scored<T>> scored = new ArrayList<>();
    for (T item : items) {
      int s = score(pattern, labelGetter.apply(item));
      if (s >= 0) {
        scored.add(new Scored<>(item, s));
      }
    }
    scored.sort(Comparator.comparingInt((Scored<T> s) -> s.score).reversed());
    List<T> result = new ArrayList<>(scored.size());
    for (Scored<T> s : scored) {
      result.add(s.item);
    }
    return result;
  }

  private static class Scored<T> {
    final T item;
    final int score;

    Scored(T item, int score) {
      this.item = item;
      this.score = score;
    }
  }
}
