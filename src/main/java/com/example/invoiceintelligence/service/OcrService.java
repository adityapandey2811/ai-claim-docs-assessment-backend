package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.config.AppConfig;
import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final AppConfig appConfig;
    private ITesseract tesseract;
    private boolean tessdataLoaded = false;

    public OcrService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @PostConstruct
    public void setup() {
        tesseract = new Tesseract();
        tesseract.setLanguage("eng");

        String configuredPath = StringUtils.hasText(appConfig.getTessDataPath())
                ? appConfig.getTessDataPath()
                : System.getenv("TESSDATA_PREFIX");

        Path tessDataFolder = findTessDataPath(configuredPath);
        if (tessDataFolder == null) {
            log.warn("Tesseract data path could not be resolved. Checked configured path '{}', " +
                    "TESSDATA_PREFIX env var, and common OS-default install locations. " +
                    "OCR on scanned images/PDFs will be UNAVAILABLE until this is fixed. " +
                    "Set app.tesseract.data-path (application.yml) or the TESSDATA_PREFIX " +
                    "environment variable to the folder containing your .traineddata files.", configuredPath);
            return;
        }

        tesseract.setDatapath(tessDataFolder.toAbsolutePath().toString());
        tessdataLoaded = true;
        log.info("Tesseract datapath configured: {}", tessDataFolder.toAbsolutePath());
    }

    private Path findTessDataPath(String configuredPath) {
        List<Path> candidates = new ArrayList<>();

        // 1. Explicitly configured path (application.yml or TESSDATA_PREFIX env var).
        //    Accept it whether or not it already ends in "tessdata".
        if (StringUtils.hasText(configuredPath)) {
            candidates.add(Path.of(configuredPath));
            candidates.add(Path.of(configuredPath, "tessdata"));
        }

        // 2. Common OS-default install locations, checked regardless of host OS
        //    (harmless if they don't exist on the current platform).
        candidates.add(Path.of("C:\\Program Files\\Tesseract-OCR\\tessdata"));
        candidates.add(Path.of("C:\\Program Files (x86)\\Tesseract-OCR\\tessdata"));
        candidates.add(Path.of("/usr/share/tesseract-ocr/5/tessdata"));
        candidates.add(Path.of("/usr/share/tesseract-ocr/4.00/tessdata"));
        candidates.add(Path.of("/usr/share/tessdata"));
        candidates.add(Path.of("/usr/local/share/tessdata"));
        candidates.add(Path.of("/opt/homebrew/share/tessdata"));       // macOS ARM (Homebrew)
        candidates.add(Path.of("/usr/local/opt/tesseract/share/tessdata")); // macOS Intel (Homebrew)

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && hasTrainedData(candidate)) {
                return candidate;
            }
        }

        // Fall back to first existing directory even without a verified .traineddata
        // file, so we still surface a datapath (Tesseract's own error will be clearer
        // than silently disabling OCR) instead of giving up entirely.
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                log.warn("Found tessdata directory {} but no .traineddata files inside it - " +
                        "OCR calls will likely fail. Verify language data is installed.", candidate);
                return candidate;
            }
        }

        return null;
    }

    private boolean hasTrainedData(Path dir) {
        try (var stream = Files.list(dir)) {
            return stream.anyMatch(p -> p.getFileName().toString().endsWith(".traineddata"));
        } catch (IOException e) {
            return false;
        }
    }

    public String extractText(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Uploaded file has no name");
        }
        String filename = StringUtils.cleanPath(originalFilename);
        if (filename.toLowerCase().endsWith(".pdf")) {
            return extractTextFromPdf(file);
        }
        if (filename.toLowerCase().endsWith(".docx")) {
            return extractTextFromDocx(file);
        }
        if (!tessdataLoaded) {
            throw new IllegalStateException(
                    "Tesseract is not configured, so image OCR is unavailable. " +
                            "Set app.tesseract.data-path or the TESSDATA_PREFIX environment variable " +
                            "to a directory containing eng.traineddata. " +
                            "Uploading a text-based PDF will still work without OCR.");
        }
        return extractTextFromImage(file);
    }

    private String extractTextFromPdf(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (tessdataLoaded) {
                    try {
                        PDFRenderer renderer = new PDFRenderer(document);
                        StringBuilder textBuilder = new StringBuilder();
                        for (int page = 0; page < document.getNumberOfPages(); page++) {
                            BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
                            String pageText = tesseract.doOCR(image);
                            textBuilder.append(pageText).append("\n");
                        }
                        String ocrText = textBuilder.toString().trim();
                        if (!ocrText.isEmpty()) {
                            return ocrText;
                        }
                    } catch (TesseractException e) {
                        log.warn("Tesseract OCR failed on PDF, falling back to embedded text extraction", e);
                    }
                } else {
                    log.info("Tesseract not configured - using embedded PDF text only (scanned pages will be blank).");
                }

                String embeddedText = extractTextFromPdfWithTextStripper(document);
                if (StringUtils.hasText(embeddedText)) {
                    return embeddedText;
                }

                throw new IllegalStateException(
                        "PDF has no embedded text and OCR is not available. " +
                                "This is likely a scanned PDF - configure Tesseract to process it.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }

    private String extractTextFromPdfWithTextStripper(PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract embedded text from PDF", e);
        }
    }

    private String extractTextFromImage(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported image format");
            }
            return tesseract.doOCR(image);
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("Failed to extract text from image", e);
        }
    }

    private String extractTextFromDocx(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            if (StringUtils.hasText(text)) {
                return text.trim();
            }
            throw new IllegalStateException("DOCX file contained no extractable text");
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from DOCX", e);
        }
    }
}
