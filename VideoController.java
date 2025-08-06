@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/batch-process")
    public ResponseEntity<?> processMultipleVideos(@RequestParam("files") List<MultipartFile> files) {
        try {
            List<String> downloadUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = videoService.processVideo(file);
                downloadUrls.add(url);
            }
            return ResponseEntity.ok(Map.of("downloads", downloadUrls));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
