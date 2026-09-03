package weather

/** 一天的天气预报。温度以摄氏度为单位。 */
data class DayForecast(val day: String, val high: Int, val low: Int, val condition: String)

/**
 * 根据**打包的示例数据**按城市提供天气预报，因此无需网络即可在任何地方运行。
 *
 * 要把它变成真实应用，可以用天气 API 调用来替换 [forecast]：通过 HTTP 拉取该城市的 JSON，
 * 解析成 `DayForecast` 并返回。其它所有部分 —— 格式化与命令循环 —— 都保持不变，
 * 因为它们只依赖 `DayForecast` 模型，而不依赖数据从何而来。
 */
object WeatherService {
    private val data = mapOf(
        "london" to listOf(
            DayForecast("Mon", 18, 11, "Cloudy"),
            DayForecast("Tue", 17, 10, "Rain"),
            DayForecast("Wed", 19, 12, "Sunny"),
        ),
        "tokyo" to listOf(
            DayForecast("Mon", 26, 19, "Sunny"),
            DayForecast("Tue", 24, 18, "Cloudy"),
            DayForecast("Wed", 22, 17, "Rain"),
        ),
        "cairo" to listOf(
            DayForecast("Mon", 34, 22, "Sunny"),
            DayForecast("Tue", 35, 23, "Sunny"),
            DayForecast("Wed", 33, 21, "Clear"),
        ),
    )

    /** 返回 [city]（不区分大小写）的 3 天天气预报；如果没有对应数据则返回 null。 */
    fun forecast(city: String): List<DayForecast>? = data[city.trim().lowercase()]

    /** 返回该示例拥有数据的城市列表。 */
    fun cities(): List<String> = data.keys.toList()
}

/** [forecast] 的单行摘要，例如 `Mon: Sunny, 11-18°C`。 */
fun format(forecast: DayForecast): String =
    "${forecast.day}: ${forecast.condition}, ${forecast.low}-${forecast.high}°C"
