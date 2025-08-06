@Service
public class ACRCloudService {

    private static final String HOST = "identify-eu-west-1.acrcloud.com";
    private static final String ACCESS_KEY = "YOUR_ACCESS_KEY";
    private static final String ACCESS_SECRET = "YOUR_ACCESS_SECRET";

    public List<VideoService.TimeRange> scanAudio(File audioFile) throws Exception {
        byte[] data = Files.readAllBytes(audioFile.toPath());
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String stringToSign = "POST\n/v1/identify\n" + ACCESS_KEY + "\naudio\n1\n" + timestamp;
        String signature = generateSignature(stringToSign, ACCESS_SECRET);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + HOST + "/v1/identify"))
                .header("access-key", ACCESS_KEY)
                .header("signature", signature)
                .header("timestamp", timestamp)
                .header("data-type", "audio")
                .header("signature-version", "1")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        // You would parse the JSON here and return the correct range.
        // For now, simulate:
        return List.of(new VideoService.TimeRange(10, 25));
    }

    private String generateSignature(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rawHmac);
    }
}
