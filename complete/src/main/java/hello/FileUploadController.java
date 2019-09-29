package hello;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import hello.storage.StorageFileNotFoundException;
import hello.storage.StorageService;

@Controller
public class FileUploadController {

	private final StorageService storageService;

	private static final String EXTENSION = ".jpg";
	private static final String SERVER_LOCATION = "upload-dir";

	@RequestMapping(path = "/download", method = RequestMethod.GET)
	public ResponseEntity<Resource> download(@RequestParam("image") String image) throws IOException {
		File file = new File(SERVER_LOCATION + File.separator + image + EXTENSION);

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=img.jpg");
		header.add("Cache-Control", "no-cache, no-store, must-revalidate");
		header.add("Pragma", "no-cache");
		header.add("Expires", "0");

		Path path = Paths.get(file.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		return ResponseEntity.ok()
				.headers(header)
				.contentLength(file.length())
				.contentType(MediaType.parseMediaType("application/octet-stream"))
				.body(resource);
	}

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) throws IOException {

		model.addAttribute("files", storageService.loadAll().map(
				path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
						"serveFile", path.getFileName().toString()).build().toString())
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

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {

		System.out.println("--- UPLOADING A FILE ---");

		storageService.store(file);
		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

		return "redirect:/";
	}

	@RequestMapping(value = "/magic", method = RequestMethod.POST)
	public @ResponseBody String uploadImage2(@RequestParam("image_key") String imageValue,
			HttpServletRequest request,
			@RequestParam("name_key") String imageName) {
		try {

			System.out.println(" *-*-*-*-*   MAGIC IS HAPPENING -*-*-*-*-");

			imageName = "blabla.jpeg";

			System.out.println("Name: " + imageName);
			//This will decode the String which is encoded by using Base64 class
			byte[] imageByte = Base64.decodeBase64(imageValue);

			String directory = "upload-dir/" + imageName;

			FileOutputStream f = new FileOutputStream(directory);
			f.write(imageByte);
			f.close();

			// process image 

			//			Process p = Runtime.getRuntime().exec("python yourapp.py");

			File workingDirectory = new File("upload-dir");

			ProcessBuilder processBuilder = new ProcessBuilder().directory(workingDirectory);
			//			processBuilder.command("cmd.exe", "/c", "python process.py " + imageName);
			processBuilder.command("/bin/bash", "-c", "python process.py " + imageName);
			Process process = processBuilder.start();
			int exitCode = process.waitFor(); // returns 0 on success, 1 on failure

			System.out.println("Exit code: " + exitCode);

			return "success ";
		}
		catch (Exception e) {
			return "error = " + e;
		}

	}

	//	@GetMapping("processedimage")
	//	public File test() {
	//
	//		return null;
	//
	//	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

}
