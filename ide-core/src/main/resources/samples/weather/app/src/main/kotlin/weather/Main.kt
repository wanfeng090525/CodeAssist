package weather

/**
 * 一个交互式天气查询工具：在 `>` 提示符下输入城市名即可查看其 3 天天气预报。输入 `quit`
 * （或发送输入结束信号）可退出。对于未知城市，会打印我们拥有数据的城市列表。
 */
fun main() {
    println("Weather — enter a city (${WeatherService.cities().joinToString(", ")}), or 'quit'.")

    while (true) {
        print("> ")
        System.out.flush() // 在阻塞等待输入之前先显示提示符
        val city = readLine()?.trim() ?: break // 输入结束

        if (city.isEmpty()) continue
        if (city.equals("quit", ignoreCase = true) || city.equals("exit", ignoreCase = true)) break

        val forecast = WeatherService.forecast(city)
        if (forecast == null) {
            println("No data for \"$city\". Try: ${WeatherService.cities().joinToString(", ")}")
        } else {
            println("3-day forecast for $city:")
            for (day in forecast) println("  " + format(day))
        }
    }

    println("Bye!")
}
