package net.azisaba.data.examination

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.examination.AbstractExaminer
import net.kyori.examination.Examinable
import net.kyori.examination.ExaminableProperty
import org.jetbrains.annotations.ApiStatus
import java.util.function.IntFunction
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

/**
 * An examiner which produces a syntax-highlighted [Component] as the output.
 *
 * @property colorScheme the colors used for syntax highlighting
 */
@ApiStatus.Experimental
class ComponentExaminer(val colorScheme: ColorScheme) : AbstractExaminer<Component>() {
    override fun examine(value: Boolean): Component {
        return Component.text(value.toString(), colorScheme.keyword)
    }

    override fun examine(value: Byte): Component {
        return Component.text(value.toString(), colorScheme.number)
    }

    override fun examine(value: Char): Component {
        return Component.text("'${escape(value.toString())}'", colorScheme.string)
    }

    override fun examine(value: Double): Component {
        return Component.text("${value}d", colorScheme.number)
    }

    override fun examine(value: Float): Component {
        return Component.text("${value}f", colorScheme.number)
    }

    override fun examine(value: Int): Component {
        return Component.text(value.toString(), colorScheme.number)
    }

    override fun examine(value: Long): Component {
        return Component.text("${value}L", colorScheme.number)
    }

    override fun examine(value: Short): Component {
        return Component.text(value.toString(), colorScheme.number)
    }

    override fun examine(value: String?): Component {
        return value?.let {
            Component.text("\"${escape(it)}\"", colorScheme.string)
        } ?: nil()
    }

    override fun <E : Any?> array(array: Array<out E>, elements: Stream<Component>): Component {
        return enclosed("[", "]", elements.toList())
    }

    override fun <E : Any?> collection(collection: Collection<E>, elements: Stream<Component>): Component {
        return enclosed("[", "]", elements.toList())
    }

    override fun examinable(name: String, properties: Stream<Map.Entry<String, Component>>): Component {
        val entries = properties.map { (property, value) ->
            Component.text()
                .append(Component.text(property, colorScheme.property))
                .append(Component.text("=", colorScheme.punctuation))
                .append(value)
                .build()
        }.toList()
        return Component.text()
            .append(Component.text(name, colorScheme.type))
            .append(enclosed("{", "}", entries))
            .build()
    }

    override fun <K : Any?, V : Any?> map(map: Map<K, V>, entries: Stream<Map.Entry<Component, Component>>): Component {
        val components = entries.map { (key, value) ->
            Component.text()
                .append(key)
                .append(Component.text("=", colorScheme.punctuation))
                .append(value)
                .build()
        }.toList()
        return enclosed("{", "}", components)
    }

    override fun nil(): Component {
        return Component.text("null", colorScheme.keyword)
    }

    override fun scalar(value: Any): Component {
        return Component.text(value.toString(), colorScheme.scalar)
    }

    override fun <T : Any?> stream(stream: Stream<T?>): Component {
        return enclosed("[", "]", stream.map(::examine).toList())
    }

    override fun stream(stream: DoubleStream): Component {
        return enclosed("[", "]", stream.mapToObj(::examine).toList())
    }

    override fun stream(stream: IntStream): Component {
        return enclosed("[", "]", stream.mapToObj(::examine).toList())
    }

    override fun stream(stream: LongStream): Component {
        return enclosed("[", "]", stream.mapToObj(::examine).toList())
    }

    override fun array(length: Int, value: IntFunction<Component>): Component {
        return enclosed("[", "]", List(length, value::apply))
    }

    private fun enclosed(open: String, close: String, elements: List<Component>): Component {
        return Component.text { builder ->
            builder.append(Component.text(open, colorScheme.punctuation))
            elements.forEachIndexed { index, element ->
                if (index > 0) {
                    builder.append(Component.text(", ", colorScheme.punctuation))
                }
                builder.append(element)
            }
            builder.append(Component.text(close, colorScheme.punctuation))
        }
    }

    private fun escape(value: String): String {
        return buildString(value.length) {
            value.forEach { character ->
                append(
                    when (character) {
                        '\\' -> "\\\\"
                        '"' -> "\\\""
                        '\'' -> "\\'"
                        '\b' -> "\\b"
                        '\u000C' -> "\\f"
                        '\n' -> "\\n"
                        '\r' -> "\\r"
                        '\t' -> "\\t"
                        else -> character
                    }
                )
            }
        }
    }

    companion object {
        private val SIMPLE: ComponentExaminer = ComponentExaminer(
            ColorScheme(
                type = NamedTextColor.AQUA,
                property = NamedTextColor.YELLOW,
                string = NamedTextColor.GREEN,
                number = NamedTextColor.GOLD,
                keyword = NamedTextColor.LIGHT_PURPLE,
                scalar = NamedTextColor.WHITE,
                punctuation = NamedTextColor.DARK_GRAY,
            )
        )

        /**
         * Returns a shared [ComponentExaminer] using the default color scheme.
         *
         * @return the shared component examiner
         */
        fun simple(): ComponentExaminer = SIMPLE
    }

    /**
     * Represents the colors used for syntax highlighting.
     *
     * @property type examinable type names
     * @property property examinable property names
     * @property string string and character literals
     * @property number numeric values
     * @property keyword boolean and null literals
     * @property scalar values without a specialized representation
     * @property punctuation delimiters, separators, and assignment operators
     *
     * @see ComponentExaminer
     */
    @Serializable
    data class ColorScheme(
        val type: TextColor,
        val property: TextColor,
        val string: TextColor,
        val number: TextColor,
        val keyword: TextColor,
        val scalar: TextColor,
        val punctuation: TextColor,
    ) : Examinable {
        override fun examinableProperties(): Stream<out ExaminableProperty?> {
            return Stream.of(
                ExaminableProperty.of("type", type),
                ExaminableProperty.of("property", property),
                ExaminableProperty.of("string", string),
                ExaminableProperty.of("number", number),
                ExaminableProperty.of("keyword", keyword),
                ExaminableProperty.of("scalar", scalar),
                ExaminableProperty.of("punctuation", punctuation),
            )
        }
    }
}
