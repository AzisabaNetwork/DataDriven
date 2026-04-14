package net.azisaba.datadriven.config

fun interface ConfigurationHolder<T> {
    fun config(): T

    fun <S> map(selector: T.() -> S): ConfigurationHolder<S> = ConfigurationHolder {
        config().let(selector)
    }
}
