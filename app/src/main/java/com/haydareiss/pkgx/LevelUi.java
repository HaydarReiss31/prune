package com.haydareiss.pkgx;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

/** Text and colors for the removal-level tags. */
final class LevelUi {

    private LevelUi() {
    }

    static int nameRes(UadInfo.Level level) {
        switch (level) {
            case RECOMMENDED:
                return R.string.level_recommended;
            case ADVANCED:
                return R.string.level_advanced;
            case EXPERT:
                return R.string.level_expert;
            case UNSAFE:
                return R.string.level_unsafe;
            default:
                return R.string.level_unlisted;
        }
    }

    private static int bgRes(UadInfo.Level level) {
        switch (level) {
            case RECOMMENDED:
                return R.color.level_recommended_bg;
            case ADVANCED:
                return R.color.level_advanced_bg;
            case EXPERT:
                return R.color.level_expert_bg;
            case UNSAFE:
                return R.color.level_unsafe_bg;
            default:
                return R.color.level_unlisted_bg;
        }
    }

    private static int fgRes(UadInfo.Level level) {
        switch (level) {
            case RECOMMENDED:
                return R.color.level_recommended_fg;
            case ADVANCED:
                return R.color.level_advanced_fg;
            case EXPERT:
                return R.color.level_expert_fg;
            case UNSAFE:
                return R.color.level_unsafe_fg;
            default:
                return R.color.level_unlisted_fg;
        }
    }

    /** Fills a tag TextView (background is the bg_tag shape) for the given level. */
    static void style(TextView tag, UadInfo.Level level) {
        Context c = tag.getContext();
        tag.setText(nameRes(level));
        tag.setTextColor(c.getColor(fgRes(level)));
        tag.setBackgroundTintList(ColorStateList.valueOf(c.getColor(bgRes(level))));
    }
}
