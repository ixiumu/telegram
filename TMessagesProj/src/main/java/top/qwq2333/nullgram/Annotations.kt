

package top.qwq2333.nullgram

/**
 * The field's Int getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class IntConfig(val defaultValue: Int)

/**
 * The field's Boolean getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class BooleanConfig(val defaultValue: Boolean = false)

/**
 * The field's String getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class StringConfig(val defaultValue: String)

/**
 * The field's Float getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class FloatConfig(val defaultValue: Float)
