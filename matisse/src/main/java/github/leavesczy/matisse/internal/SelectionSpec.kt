package github.leavesczy.matisse.internal

import github.leavesczy.matisse.Matisse

/**
 * @Author: CZY
 * @Date: 2022/6/6 15:44
 * @Desc:
 */
internal object SelectionSpec {

    private var matisseCache: Matisse? = null

    fun inject(matisse: Matisse) {
        matisseCache = matisse
    }

    fun getMatisse(): Matisse {
        return matisseCache ?: Matisse()
    }

}