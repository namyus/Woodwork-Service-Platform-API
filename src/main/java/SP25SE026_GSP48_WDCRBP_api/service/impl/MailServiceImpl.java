package SP25SE026_GSP48_WDCRBP_api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class MailServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    private static final String BREVO_SEND_EMAIL_ENDPOINT = "/smtp/email";

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.brevo.com/v3")
            .build();

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.from-email}")
    private String fromEmail;

    @Value("${brevo.from-name:Woodwork Service Platform}")
    private String fromName;

    public void sendEmail(String to, String subject, String messageType, String linkOrPassword) {
        Objects.requireNonNull(to, "Recipient email must not be null");
        Objects.requireNonNull(subject, "Email subject must not be null");

        String htmlContent = generateEmailContent(messageType, linkOrPassword);

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "name", fromName,
                        "email", fromEmail
                ),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlContent
        );

        try {
            String responseBody = restClient.post()
                    .uri(BREVO_SEND_EMAIL_ENDPOINT)
                    .header("api-key", brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info("Email accepted by Brevo. Response: {}", responseBody);
        } catch (RestClientResponseException e) {
            log.error("Brevo rejected the email request. HTTP {}: {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IllegalStateException(
                    "Failed to send email via Brevo (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (RestClientException e) {
            log.error("Could not connect to Brevo API", e);
            throw new IllegalStateException("Failed to send email via Brevo: " + e.getMessage(), e);
        }
    }

    private String generateEmailContent(String type, String data) {
        String title;
        String recipientName = "bạn";
        String messageBody;
        String coreContent;
        String safeData = escapeHtml(data == null ? "" : data);
        String normalizedType = type == null ? "" : type.toLowerCase(Locale.ROOT);

        switch (normalizedType) {
            case "otp" -> {
                title = "Mã xác thực OTP";
                messageBody = "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi. Hãy sử dụng mã OTP dưới đây để hoàn tất quy trình xác thực.";
                coreContent = "<p style='margin-top: 60px; font-size: 40px; font-weight: 600; letter-spacing: 25px; color: #ba3d4f;'>"
                        + safeData + "</p>";
            }
            case "password" -> {
                title = "Mật khẩu truy cập hệ thống";
                messageBody = "Mật khẩu truy cập hệ thống của bạn là:";
                coreContent = "<p style='margin-top: 40px; font-size: 24px; font-weight: bold; color: #1f1f1f;'>"
                        + safeData + "</p>";
            }
            case "payment" -> {
                title = "Yêu cầu thanh toán đơn hàng";
                messageBody = "Hãy nhấp vào nút bên dưới để tiến hành thanh toán đơn hàng của bạn.";
                coreContent = "<a href='" + safeData + "' style='display:inline-block;margin-top:40px;padding:12px 30px;background-color:#ba3d4f;color:#fff;font-weight:500;border-radius:8px;text-decoration:none;'>Thanh toán ngay</a>";
            }
            case "status-rejection" -> {
                title = "Cập nhật trạng thái bị từ chối";
                recipientName = "xưởng mộc";
                messageBody = "Yêu cầu cập nhật trạng thái của bạn đã bị từ chối.<br><br><strong>Lý do:</strong> " + safeData;
                coreContent = "";
            }
            case "buy-pack-success" -> {
                title = "Gói dịch vụ đã được kích hoạt";
                recipientName = "xưởng mộc";
                messageBody = "Cảm ơn bạn đã mua gói dịch vụ. Gói của bạn là:";
                coreContent = "<p style='margin-top: 40px; font-size: 20px; font-weight: bold; color: #1f1f1f;'>"
                        + safeData + "</p>";
            }
            default -> {
                title = "Thông báo hệ thống";
                messageBody = "Đây là thông báo từ hệ thống.";
                coreContent = "";
            }
        }

        return "<!DOCTYPE html>"
                + "<html lang='vi'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Email Notification</title></head>"
                + "<body style='margin: 0; font-family: Arial, sans-serif; background: #ffffff; font-size: 14px;'>"
                + "<div style='max-width: 680px; margin: 0 auto; padding: 45px 30px 60px; background: #f4f7ff; "
                + "background-image: url(https://archisketch-resources.s3.ap-northeast-2.amazonaws.com/vrstyler/1661497957196_595865/email-template-background-banner);"
                + "background-repeat: no-repeat; background-size: 800px 452px; background-position: top center; color: #434343;'>"
                + "<header><table style='width: 100%;'><tr><td>"
                + "<img src='https://woodwork-service-platform-web-app.vercel.app/Ke4MgOK.png' height='30' alt='Woodwork Service Platform logo'/>"
                + "</td><td style='text-align: right;'><span style='font-size: 16px; line-height: 30px; color: #ffffff;'>"
                + LocalDate.now()
                + "</span></td></tr></table></header>"
                + "<main><div style='margin-top: 70px; padding: 92px 30px 115px; background: #ffffff; border-radius: 30px; text-align: center;'>"
                + "<div style='max-width: 489px; margin: 0 auto;'>"
                + "<h1 style='font-size: 24px; font-weight: 500; color: #1f1f1f; margin-bottom: 20px;'>" + title + "</h1>"
                + "<p style='font-size: 16px; font-weight: 500;'>Chào " + recipientName + ",</p>"
                + "<p style='margin-top: 17px; font-weight: 500; letter-spacing: 0.56px;'>" + messageBody + "</p>"
                + coreContent
                + "</div></div>"
                + "<p style='max-width: 400px; margin: 90px auto 0; text-align: center; font-weight: 500; color: #8c8c8c;'>"
                + "Cần trợ giúp? Gửi email đến <a href='mailto:namndpse171442@fpt.edu.vn' style='color: #499fb6;'>namndpse171442@fpt.edu.vn</a>"
                + "</p></main>"
                + "<footer style='max-width: 490px; margin: 20px auto 0; text-align: center; border-top: 1px solid #e6ebf1;'>"
                + "<p style='margin-top: 40px; font-size: 16px; font-weight: 600;'>Nền tảng đồ gỗ</p>"
                + "<p style='margin-top: 8px;'>Địa chỉ: 123 Đường Gỗ Việt, Q.Tân Bình, TP.HCM</p>"
                + "<p style='margin-top: 16px;'>© 2026 Nền tảng đồ gỗ. All rights reserved.</p>"
                + "</footer></div></body></html>";
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
