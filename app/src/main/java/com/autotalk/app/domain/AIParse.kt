package com.autotalk.app.domain

/** 从模型输出中抽取 JSON 字符串（容忍前后多余文字与 ```json 代码块）。 */
object AIParse {
    fun extractJsonObject(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            val firstNewline = s.indexOf('\n')
            if (firstNewline >= 0) s = s.substring(firstNewline + 1)
            if (s.endsWith("```")) s = s.removeSuffix("```")
            s = s.trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start in 0..end) s = s.substring(start, end + 1)
        return s
    }
}
