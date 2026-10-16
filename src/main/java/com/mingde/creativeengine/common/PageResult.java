package com.mingde.creativeengine.common;

import java.util.List;

/**
 * 分页结果。
 *
 * <p>显式带上 {@code hasMore}，而不是让前端自己算 {@code page * size < total}：
 * 多端各自算容易在边界上算错，而且以后若改成游标分页，前端不用改判断逻辑。</p>
 */
public record PageResult<T>(List<T> list, long total, long page, long size, boolean hasMore) {

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        return new PageResult<>(list, total, page, size, page * size < total);
    }
}
