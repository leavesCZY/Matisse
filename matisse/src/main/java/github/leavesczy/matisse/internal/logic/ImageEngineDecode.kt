package github.leavesczy.matisse.internal.logic

import androidx.compose.ui.unit.Constraints

internal object ImageEngineDecode {

    /** 单张位图宽/高上限，用于预览大图与异常超大约束。 */
    const val MAX_BITMAP_DIMENSION = 4096

    /**
     * 按 Compose 约束计算目标解码宽高，并限制在 [MAX_BITMAP_DIMENSION] 以内。
     * 某一方向无界时使用 [MediaStoreThumbnail.MAX_DIMENSION]。
     */
    fun targetSize(constraints: Constraints): Pair<Int, Int> {
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth.coerceIn(
                minimumValue = 1,
                maximumValue = MAX_BITMAP_DIMENSION
            )
        } else {
            MediaStoreThumbnail.MAX_DIMENSION
        }
        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight.coerceIn(
                minimumValue = 1,
                maximumValue = MAX_BITMAP_DIMENSION
            )
        } else {
            MediaStoreThumbnail.MAX_DIMENSION
        }
        return width to height
    }

    /**
     * 预览大图：宽度取容器宽度（有上限），高度不按容器限制，由库按比例采样，
     * 再靠各自引擎的 maxBitmap / override 高度上限兜底。
     */
    fun previewWidth(constraints: Constraints): Int {
        return if (constraints.hasBoundedWidth) {
            constraints.maxWidth.coerceIn(
                minimumValue = 1,
                maximumValue = MAX_BITMAP_DIMENSION
            )
        } else {
            MAX_BITMAP_DIMENSION
        }
    }

}
