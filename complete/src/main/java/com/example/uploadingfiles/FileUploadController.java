package com.example.uploadingfiles;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.UUID;

// Spring Framework
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Apache Tika
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.apache.tika.metadata.Metadata;

// JodConverter
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;

import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;

import com.example.general.logger.SimpleLogger;

@Controller
public class FileUploadController {

	private final StorageService storageService;
	private final SimpleLogger logger = new SimpleLogger();

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) throws IOException {

		model.addAttribute("files", storageService.loadAll().map(
			path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
					"serveFile", path.getFileName().toString()).build().toUri().toString())
			.collect(Collectors.toList()));

		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@GetMapping("/files/get/text/{filename:.+}")
	@ResponseBody
	public String getText(@PathVariable String filename, Model model) {
		// Apache Tika での HTML 化
		AutoDetectParser parser = new AutoDetectParser();
		Metadata metaData = new Metadata();
		ParseContext context = new ParseContext();
		BodyContentHandler handler = new BodyContentHandler();
		// ToHTMLContentHandler handler = new ToHTMLContentHandler();

		String text = "unloaded";
		try {
			Path file = storageService.load(filename);
			FileInputStream stream = new FileInputStream(file.toFile());
			parser.parse(stream, handler, metaData, context);

			text = handler.toString();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		model.addAttribute("text", text);
		return text;
	}

	/*
	 * PDF プレビューサンプル
	 *
	 * [require]: libreoffice インストール
	 * [back]   : PDF を作成
	 * [back]   : ファイル URL/ファイル名を送信する
	 * [front]  : PDF.js でプレビュー表示
	 * [front]  : プレビュー閉じたら PDF ファイルを削除
	 */
	@GetMapping("/files/preview/{filename:.+}")
	@ResponseBody
	public String makePDF(@PathVariable String filename) {
		try {
			Path inputFilePath = storageService.load(filename);
			logger.printLog(SimpleLogger.LOG_LEVEL.DEBUG, inputFilePath.toString());
			File inputFile = inputFilePath.toFile();

			UUID uuid = UUID.randomUUID();
			String outputFileName = "preview-" + uuid.toString() + ".pdf";
			String outputFilePathString = inputFilePath
				.toAbsolutePath().toString()
				.replace(inputFilePath.getFileName().toString(), outputFileName);
			File outputFile = new File(outputFilePathString);

			logger.printLog(SimpleLogger.LOG_LEVEL.INFO, "Convert start.");
			OfficeManager officeManager = LocalOfficeManager.make();
			DocumentConverter converter = LocalConverter.make(officeManager);

			officeManager.start();
			converter.convert(inputFile).to(outputFile).execute();
			officeManager.stop();
			logger.printLog(SimpleLogger.LOG_LEVEL.INFO, "Convert end.");

			String outputURL = MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
				"serveFile", outputFileName).build().toUri().toString();
			return "{\n\"result\": \"ok\",\n\"url\": \"" + outputURL + "\"\n}";
		} catch (Exception e) {
			logger.printLog(SimpleLogger.LOG_LEVEL.ERROR, e.toString());
		}

		return "{\n\"result\": \"ng\"\n}";
	}

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {

		storageService.store(file);
		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

		return "redirect:/";
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

}
