package co.mipyme.ui.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoshiSpacing(
    val space0: Dp = 0.dp,
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space5: Dp = 20.dp,
    val space6: Dp = 24.dp,
    val space8: Dp = 32.dp,
    val space10: Dp = 40.dp,
    val space12: Dp = 48.dp,
    val space16: Dp = 64.dp,

    val pageGutterNarrow: Dp = space4,
    val pageGutterMedium: Dp = space6,
    val pageGutterWide: Dp = space8,
    val pageSectionGapCompact: Dp = space8,
    val pageSectionGapComfortable: Dp = space12,
    val sectionHeaderGap: Dp = space4,
    val gridGapCompact: Dp = space3,
    val gridGapComfortable: Dp = space6,
)
