package a88.jbay.util;

import a88.jbay.common.auction.BidTransaction;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChartHelper {
    // Gom Magic Number (15) và Magic String format vào hằng số để dễ quản lý
    private static final int MAX_DATA_POINTS = 15;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Hàm cấu hình LineChart cơ bản, trả về Series để Controller giữ
     */
    public static XYChart.Series<String, Number> setupPriceChart(LineChart<String, Number> chart, String seriesName) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);
        chart.getData().add(series);
        chart.setAnimated(false); // Tắt animation để cập nhật mượt hơn
        return series;
    }

    /**
     * Hàm nhận dữ liệu từ Server và tự động vẽ lại lên biểu đồ
     */
    public static void updatePriceChart(XYChart.Series<String, Number> series, List<BidTransaction> bidHistory) {
        series.getData().clear();

        for (BidTransaction bid : bidHistory) {
            String time = bid.getTimestamp().format(TIME_FORMATTER);
            series.getData().add(new XYChart.Data<>(time, bid.getAmt()));
        }

        // Tự động cắt đuôi nếu quá giới hạn điểm hiển thị
        if (series.getData().size() > MAX_DATA_POINTS) {
            series.getData().remove(0, series.getData().size() - MAX_DATA_POINTS);
        }
    }
}
