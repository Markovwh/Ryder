package common.ui.pages.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

@Composable
fun HashtagText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val onHashtagClick = LocalHashtagClickHandler.current
    val hashtagRegex = remember { Regex("#(\\w+)") }

    val annotatedString = remember(text) {
        buildAnnotatedString {
            var lastIndex = 0
            hashtagRegex.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))
                pushStringAnnotation(tag = "HASHTAG", annotation = match.groupValues[1])
                pushStyle(SpanStyle(color = RyderAccent, fontWeight = FontWeight.SemiBold))
                append(match.value)
                pop()
                pop()
                lastIndex = match.range.last + 1
            }
            if (lastIndex < text.length) append(text.substring(lastIndex))
        }
    }

    if (onHashtagClick != null) {
        ClickableText(
            text = annotatedString,
            style = TextStyle(color = color, fontSize = fontSize),
            modifier = modifier,
            onClick = { offset ->
                annotatedString
                    .getStringAnnotations("HASHTAG", offset, offset)
                    .firstOrNull()
                    ?.let { onHashtagClick(it.item) }
            }
        )
    } else {
        Text(text = annotatedString, fontSize = fontSize, modifier = modifier)
    }
}
