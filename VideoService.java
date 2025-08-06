@Service
public class VideoService {

    private final ACRCloudService acrCloudService;

    public VideoService(ACRCloudService acrCloudService) {
        this.acrCloudService = acrCloudService;
    }

    public String processVideo(MultipartFile file) throws Exception {
        File uploaded = new File("uploads/" + file.getOriginalFilename());
        file.transferTo(uploaded);

        File sampleAudio = new File("uploads/sample.mp3");
        extractAudio(uploaded, sampleAudio);

        List<TimeRange> ranges = acrCloudService.scanAudio(sampleAudio);

        File output = new File("uploads/clean_" + file.getOriginalFilename());
        cutVideo(uploaded, output, ranges);

        return "/uploads/" + output.getName();
    }

    private void extractAudio(File video, File audio) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-i", video.getAbsolutePath(), "-t", "15",
                "-q:a", "0", "-map", "a", audio.getAbsolutePath());
        pb.inheritIO().start().waitFor();
    }

    private void cutVideo(File input, File output, List<TimeRange> remove) throws IOException, InterruptedException {
        double duration = getVideoDuration(input);

        List<TimeRange> keep = new ArrayList<>();
        double last = 0;
        for (TimeRange r : remove) {
            if (r.start > last) keep.add(new TimeRange((int) last, r.start));
            last = r.end;
        }
        if (last < duration) keep.add(new TimeRange((int) last, (int) duration));

        List<File> clips = new ArrayList<>();
        int i = 0;
        for (TimeRange r : keep) {
            File part = new File("uploads/part" + i++ + ".mp4");
            new ProcessBuilder("ffmpeg", "-y", "-i", input.getAbsolutePath(), "-ss", String.valueOf(r.start),
                    "-to", String.valueOf(r.end), "-c", "copy", part.getAbsolutePath()).inheritIO().start().waitFor();
            clips.add(part);
        }

        File listFile = new File("uploads/concat.txt");
        try (PrintWriter pw = new PrintWriter(listFile)) {
            for (File p : clips) pw.println("file '" + p.getAbsolutePath().replace("\\", "/") + "'");
        }

        new ProcessBuilder("ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i",
                listFile.getAbsolutePath(), "-c", "copy", output.getAbsolutePath())
                .inheritIO().start().waitFor();
    }

    private double getVideoDuration(File file) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries",
                "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", file.getAbsolutePath());
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String durationStr = br.readLine();
        p.waitFor();
        return Double.parseDouble(durationStr);
    }

    public static class TimeRange {
        public int start, end;
        public TimeRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
